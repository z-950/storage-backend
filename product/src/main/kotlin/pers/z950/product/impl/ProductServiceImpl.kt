package pers.z950.product.impl

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import pers.z950.common.service.ServiceException
import pers.z950.common.service.repository.PostgresRepositoryWrapper
import pers.z950.common.sql.Column
import pers.z950.common.sql.Sql
import pers.z950.common.sql.Table
import pers.z950.product.Product
import pers.z950.product.ProductService
import java.util.*

class ProductServiceImpl : PostgresRepositoryWrapper(), ProductService {
  private object TABLE : Table("product") {
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

  override suspend fun init(vertx: Vertx, config: JsonObject) {
    super.init(vertx, config)

    // todo: 外键
    val createTable = """
create table if not exists $TABLE (
  ${TABLE.id} varchar(256) primary key,
  ${TABLE.number} int default 0,
  ${TABLE.shelfId} varchar(64) not null,
  ${TABLE.regionId} int not null
);
    """
    queryAwait(createTable)

    val productList = listOf(
      Product("4a507db7-7c52-2c22-d6a6-77ade48625bd", "A", 1, 8),
      Product("33783757-e89e-3262-f0a4-e9db956ca1c3", "A", 2, 6),
      Product("79eb1d76-a37e-a575-7445-1be57fc63842", "A", 3, 2),
      Product("6aa05e8e-456e-35a5-df0a-fe1ffc2798d4", "A", 4, 6),
      Product("d3c5f8b7-b5cf-d9ec-91f2-b1db95a82cc9", "A", 5, 7),
      Product("3a8497ca-a598-ba50-dd0c-4df42d9f0532", "A", 9, 2),
      Product("fc25a168-bb33-f997-6572-8655f829361c", "A", 10, 5),
      Product("2f43da55-beb5-fe84-dbdd-0d8d04040b51", "A", 11, 6),
      Product("eefdc6d2-8dd3-11cb-d7b6-49731edc0e1d", "A", 12, 8),

      Product("8422024f-cfbc-9309-d884-07db3221fbec", "B", 1, 12),
      Product("30eef49c-ce14-3305-d813-5dbde54560d1", "B", 2, 5),
      Product("1e177873-f647-af1c-487e-c126d697e0c6", "B", 3, 12),
      Product("db332b44-85f3-c428-c131-6c7f69596214", "B", 4, 22),
      Product("e4ee88ce-fd23-43a7-fccb-fca0bd597f38", "B", 5, 3),
      Product("202fcfea-e5e7-6051-81cb-77dcbc265e50", "B", 6, 11),
      Product("9be101d5-e5b2-d805-2eed-3afa6d0e9627", "B", 7, 18),
      Product("696c347f-c48f-a2b9-855c-6a6f997aa054", "B", 8, 9),
      Product("cf160a58-8058-b217-a272-2f60091928c9", "B", 9, 2),

      Product("37fba7ba-20f6-efac-5152-411ef521a383", "C", 1, 12),
      Product("11c01bad-8319-def3-6409-d086cf5fba4c", "C", 2, 9),
      Product("3020117f-7d45-32d5-1260-25433e829d49", "C", 3, 6),
      Product("b233bb40-fbb1-81eb-a676-badb22d54aa9", "C", 4, 3),
      Product("3f008cd2-d81d-54c5-9d89-5de1254b3c1a", "C", 5, 8),
      Product("97a7c75e-dab0-6fb5-9b3d-28d1c272cb2d", "C", 6, 4),

      Product("e980f88b-2116-ffd2-aaac-47a92c092ed8", "D", 1, 2),
      Product("0594b6c4-d1b1-e2c0-36d0-e2a022b43c0c", "D", 2, 1),
      Product("ce42b3b2-a96f-a812-9216-494f33d1527c", "D", 3, 2),
      Product("8393aae1-4cbd-3323-70bf-12d871390abb", "D", 4, 3),
      Product("8aa95f76-f6b9-576d-66ef-91673817c13a", "D", 5, 9),
      Product("0df1a700-2904-9b2d-a888-3535958ff3b3", "D", 6, 6),
      Product("01ad995a-9322-9668-bd47-726377baf99c", "D", 7, 2),
      Product("fb76b14f-ba23-0825-3609-283ad7fd6001", "D", 8, 4)
    )

    productList.forEach {
      val sql = Sql(TABLE).insert(
        TABLE.id to it.id,
        TABLE.shelfId to it.shelfId,
        TABLE.regionId to it.regionId,
        TABLE.number to it.number
      ).onConflictDoNoting(TABLE.id)
      preparedQueryAwait(sql)
    }
  }

  override suspend fun getProduct(id: String): Product? {
    val sql = Sql(TABLE).select().where { with(it) { TABLE.id eq id } }
    val res = preparedQueryAwait(sql)

    if (res.size() == 0) {
      return null
    }

    val row = res.first()

    return TABLE.parse(row)
  }

  override suspend fun patchProduct() {
    TODO("Not yet implemented")
  }
}
