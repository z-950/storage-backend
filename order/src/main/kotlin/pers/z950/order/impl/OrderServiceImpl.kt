package pers.z950.order.impl

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf
import pers.z950.common.service.repository.PostgresRepositoryWrapper
import io.vertx.serviceproxy.ServiceProxyBuilder
import io.vertx.sqlclient.Row
import pers.z950.common.sql.Column
import pers.z950.common.sql.Sql
import pers.z950.common.sql.Table
import pers.z950.order.Order
import pers.z950.order.OrderService
import pers.z950.product.ProductService
import kotlin.random.Random

class OrderServiceImpl : PostgresRepositoryWrapper(), OrderService {
  private object TABLE : Table("the_order") {
    val id = Column("id")
    val productList = Column("product_list")
    val numberList = Column("number_list")
    val isChecked = Column("is_checked")
    val checker = Column("checker")

    fun parse(row: Row) = Order(
      id = row.getInteger(id.name),
      productList = row.getStringArray(productList.name).toList(),
      numberList = row.getIntegerArray(numberList.name).toList(),
      isChecked = row.getBoolean(isChecked.name),
      checker = row.getString(checker.name)
    )
  }

  private lateinit var productService: ProductService

  override suspend fun init(vertx: Vertx, config: JsonObject) {
    super.init(vertx, config)

    val createTable = """
create table if not exists $TABLE (
  ${TABLE.id} serial primary key,
  ${TABLE.productList} text[],
  ${TABLE.numberList} int[],
  ${TABLE.isChecked} bool default false,
  ${TABLE.checker} varchar(256)
);
    """
    queryAwait(createTable)

    productService = ServiceProxyBuilder(vertx).setAddress("service.product").build(ProductService::class.java)

    val notCheckedList = getAllNotChecked()

    // gen order
    if (notCheckedList.isEmpty()) {
      val productList = productService.getAllProduct()

      val orderList = mutableListOf<Map<String, Int>>()

      while (orderList.size < 5) {
        val products = mutableMapOf<String, Int>()

        // gen products
        List(Random.nextInt(1, productList.size)) { Random.nextInt(0, productList.size - 1) }
          .toSet().forEach { i ->
            val id = productList[i].id
            val num = productList[i].number
            if (num > 0) {
              val wanted = if (num > 1) {
                Random.nextInt(1, num)
              } else {
                1
              }

              orderList.mapNotNull { it[id] }.run {
                if (isNotEmpty()) {
                  if (reduce { sum, it -> sum + it } + wanted > num) {
                    // not enough
                    return@run
                  }
                }
                products[id] = wanted
              }
            }
          }

        if (products.isNotEmpty()) {
          orderList.add(products)
        }
      }

      orderList.forEach { create(it) }
    }
  }

  override suspend fun create(map: Map<String, Int>): Order {
    val sql = Sql(TABLE)
      .insert(TABLE.productList to map.keys.toTypedArray(), TABLE.numberList to map.values.toTypedArray())
      .returning(TABLE.id)
    val res = preparedQueryAwait(sql)

    return Order(res.first().getInteger(TABLE.id.name), map.keys.toList(), map.values.toList())
  }

  override suspend fun getAllNotChecked(): List<Order> {
    val sql = Sql(TABLE).select().where { with(it) { TABLE.isChecked eq false } }
    val res = preparedQueryAwait(sql)

    return res.map { TABLE.parse(it) }
  }

  override suspend fun checkOrder(id: Int, worker: String) {
    val orderRowSet = preparedQueryAwait(Sql(TABLE).select().where { with(it) { TABLE.id eq id } })

    if (orderRowSet.size() == 0) {
      return
    }

    val order = TABLE.parse(orderRowSet.first())
    productService.reduceProducts(order.productList.zip(order.numberList).toMap())
    val sql =
      Sql(TABLE).update().set(TABLE.isChecked to true, TABLE.checker to worker).where { with(it) { TABLE.id eq id } }
    preparedQueryAwait(sql)
  }
}
