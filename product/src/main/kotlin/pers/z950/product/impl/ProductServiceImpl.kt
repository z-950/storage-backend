package pers.z950.product.impl

import pers.z950.common.service.ServiceException
import pers.z950.common.service.repository.PostgresRepositoryWrapper
import pers.z950.common.sql.Column
import pers.z950.common.sql.Sql
import pers.z950.common.sql.Table
import io.vertx.sqlclient.Row
import pers.z950.product.Product
import pers.z950.product.ProductService
import java.util.*

class ProductServiceImpl : PostgresRepositoryWrapper(), ProductService {
  private object TABLE : Table("pers/z950/product") {
    val id = Column("id")
    val shelfId = Column("shelf_id")
    val regionId = Column("region_id")
    val number = Column("number")

    fun parse(row: Row) = Product(
      id = row.getString(id.name),
      shelfId = row.getString(shelfId.name),
      regionId = row.getInteger(regionId.name),
      number = row.getInteger(number.name)
    )
  }

  override suspend fun getProduct(id: String): Product {
    val sql = Sql(TABLE).select().where { with(it) { TABLE.id eq UUID.fromString(id) } }
    val res = preparedQueryAwait(sql)

    if (res.size() == 0) {
      throw ServiceException.NOT_FOUND
    }

    val data = res.first()

    return TABLE.parse(data)
  }
}
