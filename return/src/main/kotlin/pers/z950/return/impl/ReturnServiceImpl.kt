package pers.z950.`return`.impl

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.serviceproxy.ServiceProxyBuilder
import io.vertx.sqlclient.Row
import pers.z950.`return`.Return
import pers.z950.`return`.ReturnService
import pers.z950.common.service.repository.PostgresRepositoryWrapper
import pers.z950.common.sql.Column
import pers.z950.common.sql.Sql
import pers.z950.common.sql.Table
import pers.z950.order.OrderService
import pers.z950.product.ProductService

class ReturnServiceImpl : PostgresRepositoryWrapper(), ReturnService {
  private object TABLE : Table("the_return") {
    val uid = Column("uid")
    val orderId = Column("order_id")
    val productId = Column("product_id")
    val number = Column("number")
    val isChecked = Column("is_checked")
    val checker = Column("checker")

    fun parse(row: Row) = Return(
      uid = row.getInteger(uid.name),
      orderId = row.getString(orderId.name),
      productId = row.getString(productId.name),
      number = row.getInteger(number.name),
      isChecked = row.getBoolean(isChecked.name),
      checker = row.getString(checker.name)
    )
  }

  private lateinit var orderService: OrderService
  private lateinit var productService: ProductService

  override suspend fun init(vertx: Vertx, config: JsonObject) {
    super.init(vertx, config)

    val createTable = """
      create table if not exists $TABLE (
        ${TABLE.uid} serial primary key,
        ${TABLE.orderId} varchar(256),
        ${TABLE.productId} varchar(256),
        ${TABLE.number} int,
        ${TABLE.isChecked} bool default false,
        ${TABLE.checker} varchar(256)
      );
    """.trimIndent()
    queryAwait(createTable)

    orderService = ServiceProxyBuilder(vertx).setAddress("service.order").build(OrderService::class.java)
    productService = ServiceProxyBuilder(vertx).setAddress("service.product").build(ProductService::class.java)
  }

  private suspend fun create(): List<Return> {
    // mock
    val list = orderService.getAllChecked()
    return list.filter { order ->
      val sql =
        Sql(TABLE).select().where { with(it) { TABLE.orderId eq order.id and TABLE.productId eq order.product } }
      val res = preparedQueryAwait(sql)
      return@filter res.size() == 0
    }.take(2).map {
      val sql = Sql(TABLE).insert(TABLE.orderId to it.id, TABLE.productId to it.product, TABLE.number to it.number)
        .returning(TABLE.uid)
      val res = preparedQueryAwait(sql).first().getInteger(TABLE.uid.name)
      Return(res, it.id, it.product, it.number)
    }
  }

  override suspend fun getAllNotChecked(worker: String): List<Return> {
    val sql = Sql(TABLE).select().where { with(it) { TABLE.isChecked eq false } }
    val res = preparedQueryAwait(sql).map { TABLE.parse(it) }
    if (res.isEmpty()) {
      return create()
    }
    return res
  }

  override suspend fun checkReturn(uid: Int, worker: String) {
    val getSql = Sql(TABLE).select().where { with(it) { TABLE.uid eq uid } }
    val res = preparedQueryAwait(getSql).first()
    val data = TABLE.parse(res)
    productService.putProduct(data.productId, data.number)
    val sql =
      Sql(TABLE).update().set(TABLE.checker to worker, TABLE.isChecked to true).where { with(it) { TABLE.uid eq uid } }
    preparedQueryAwait(sql)
  }
}
