package pers.z950.count.impl

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import pers.z950.common.service.repository.PostgresRepositoryWrapper
import pers.z950.common.sql.Column
import pers.z950.common.sql.Sql
import pers.z950.common.sql.Table
import pers.z950.count.Count
import pers.z950.count.CountService

class CountServiceImpl : PostgresRepositoryWrapper(), CountService {
  private object TABLE : Table("the_count") {
    val id = Column("id")
    val shelfId = Column("shelf_id")
    val finished = Column("finished")
    val worker = Column("worker")

    fun parse(row: Row) = Count(
      id = row.getInteger(id.name),
      shelfId = row.getString(shelfId.name),
      finished = row.getBoolean(finished.name),
      worker = row.getString(worker.name)
    )
  }


  override suspend fun init(vertx: Vertx, config: JsonObject) {
    super.init(vertx, config)

    val createTable = """
      create table if not exists $TABLE (
        ${TABLE.id} serial primary key,
        ${TABLE.shelfId} varchar(256),
        ${TABLE.finished} bool default false,
        ${TABLE.worker} varchar(256)
      );
    """.trimIndent()
    queryAwait(createTable)
  }

  override suspend fun create(shelfId: String, worker: String) {
    val sql = Sql(TABLE).insert(TABLE.shelfId to shelfId, TABLE.worker to worker)
    preparedQueryAwait(sql)
  }

  override suspend fun getAllCount(): List<Count> {
    val sql = Sql(TABLE).select()
    return preparedQueryAwait(sql).map { TABLE.parse(it) }
  }

  override suspend fun getNotFinishedCount(worker: String): List<Count> {
    val sql = Sql(TABLE).select().where { with(it) { TABLE.worker eq worker and TABLE.finished eq false } }
    return preparedQueryAwait(sql).map { TABLE.parse(it) }
  }

  override suspend fun finishCount(id: Int) {
    val sql = Sql(TABLE).update().set(TABLE.finished to true).where { with(it) { TABLE.id eq id } }
    preparedQueryAwait(sql)
  }
}
