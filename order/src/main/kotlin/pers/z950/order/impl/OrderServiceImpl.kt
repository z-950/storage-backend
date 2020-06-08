package pers.z950.order.impl

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import pers.z950.common.service.repository.PostgresRepositoryWrapper
import io.vertx.serviceproxy.ServiceProxyBuilder
import io.vertx.sqlclient.Row
import pers.z950.common.sql.Column
import pers.z950.common.sql.Sql
import pers.z950.common.sql.Table
import pers.z950.order.Order
import pers.z950.order.OrderService
import pers.z950.product.ProductService

class OrderServiceImpl : PostgresRepositoryWrapper(), OrderService {
  private object TABLE : Table("the_order") {
    val uid = Column("uid")
    val id = Column("id")
    val product = Column("product")
    val number = Column("number")
    val isChecked = Column("is_checked")
    val checker = Column("checker")

    fun parse(row: Row) = Order(
      uid = row.getInteger(uid.name),
      id = row.getString(id.name),
      product = row.getString(product.name),
      number = row.getInteger(number.name),
      isChecked = row.getBoolean(isChecked.name),
      checker = row.getString(checker.name)
    )
  }

  private lateinit var productService: ProductService

  private val workerMaxPickSku = 5

  override suspend fun init(vertx: Vertx, config: JsonObject) {
    super.init(vertx, config)

    val createTable = """
      create table if not exists $TABLE (
        ${TABLE.uid} serial primary key,
        ${TABLE.id} varchar(256),
        ${TABLE.product} varchar(256),
        ${TABLE.number} int,
        ${TABLE.isChecked} bool default false,
        ${TABLE.checker} varchar(256)
      );
    """.trimIndent()
    queryAwait(createTable)

    productService = ServiceProxyBuilder(vertx).setAddress("service.product").build(ProductService::class.java)
  }

  override suspend fun create(id: String, list: List<Pair<String, Int>>) {
    safeSync { transaction ->
      list.forEach {
        // slice number under workerMaxPickSku
        var slice = it.second / workerMaxPickSku
        val left = it.second % workerMaxPickSku
        if (left != 0) {
          val sql = Sql(TABLE)
            .insert(
              TABLE.id to id,
              TABLE.product to it.first,
              TABLE.number to left
            )
          transaction.preparedQueryAwait(sql)
        }
        while (0 < slice--) {
          val sql = Sql(TABLE)
            .insert(
              TABLE.id to id,
              TABLE.product to it.first,
              TABLE.number to workerMaxPickSku
            )
          transaction.preparedQueryAwait(sql)
        }
      }
    }
  }

  override suspend fun getAllNotChecked(worker: String): List<List<Order>> {
    val list = safeSync { transaction ->
      val maxNewTask = 10
      val list = mutableListOf<Order>()

      val selfNotCheckedSql =
        Sql(TABLE).select().where { with(it) { TABLE.isChecked eq false and TABLE.checker eq worker } }
      val selfNotChecked = transaction.preparedQueryAwait(selfNotCheckedSql).map { TABLE.parse(it) }

      // todo: 分拣优化区域, 订单优先级
      val selfList = selfNotChecked.filter { it.checker == worker }
      if (selfList.isNotEmpty()) {
        list.addAll(selfList)
      }

      if (list.size < maxNewTask / 2) {
        val allocatableSql =
          Sql(TABLE).select().where { with(it) { TABLE.isChecked eq false and TABLE.checker.isNull() } }.forUpdate()
        val allocatableList = transaction.preparedQueryAwait(allocatableSql).map { TABLE.parse(it) }
        val newList = allocatableList.sortedBy { it.number }.takeLast(maxNewTask - list.size)
        newList.forEach { order ->
          val sql = Sql(TABLE).update().set(TABLE.checker to worker).where { with(it) { TABLE.uid eq order.uid } }
          transaction.preparedQueryAwait(sql)
        }
        list.addAll(newList)
      }

      return@safeSync list.toList()
    }

    // 相同订单合并
    val result = mutableListOf<List<Order>>()
    val map = mutableMapOf<String, MutableList<Order>>()
    list.sortedWith(compareBy({ it.id }, { it.number })).forEach { order ->
      if (map[order.id] == null) {
        map[order.id] = mutableListOf()
      }
      val curList = map[order.id]!!
      val total = curList.sumBy { it.number }
      if (total + order.number <= workerMaxPickSku) {
        curList.add(order)
      } else {
        result.add(curList.toList())
        curList.clear()
        curList.add(order)
      }
    }
    result.addAll(map.values)

    return result.toList()
  }

  override suspend fun checkOrder(uid: Int) {
    val sql = Sql(TABLE).update().set(TABLE.isChecked to true).where { with(it) { TABLE.uid eq uid } }
    preparedQueryAwait(sql)
  }
}
