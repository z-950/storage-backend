package pers.z950.common.service.repository

import pers.z950.common.service.AsyncInit
import pers.z950.common.service.Close
import pers.z950.common.sql.Sql
import pers.z950.common.sql.Table
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.kotlin.sqlclient.beginAwait
import io.vertx.kotlin.sqlclient.commitAwait
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.kotlin.sqlclient.rollbackAwait
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.*
import io.vertx.sqlclient.PoolOptions.DEFAULT_MAX_SIZE
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

abstract class PostgresRepositoryWrapper : AsyncInit,
  Close {
  companion object {
    const val DEFAULT_DATABASE = "postgres"

    const val POOL_SIZE_KEY = "poolSize"
    const val DATABASE_KEY = "database"
    const val HOST_KEY = "host"
    const val PORT_KEY = "port"
    const val USER_KEY = "user"
    const val PASSWORD_KEY = "password"
  }

  val log: Logger = LoggerFactory.getLogger(this::class.java.name)

  lateinit var vertx: Vertx
  private lateinit var pool: PgPool

  override suspend fun init(vertx: Vertx, config: JsonObject) {
    this.vertx = vertx

    val poolSize = config.getInteger(POOL_SIZE_KEY, DEFAULT_MAX_SIZE)
    val database = getNotNullConfig<String>(
      config,
      DATABASE_KEY
    )
    val host = getNotNullConfig<String>(
      config,
      HOST_KEY
    )
    val port = getNotNullConfig<Int>(
      config,
      PORT_KEY
    )
    val user = getNotNullConfig<String>(
      config,
      USER_KEY
    )
    val password = getNotNullConfig<String>(
      config,
      PASSWORD_KEY
    )

    checkAndCreateDatabase(vertx, database = database, host = host, port = port, user = user, password = password)

    connectDatabase(
      vertx,
      database = database,
      host = host,
      port = port,
      user = user,
      password = password,
      poolSize = poolSize
    )
  }

  override fun close() {
    if (::pool.isInitialized) {
      pool.close()
    }
  }

  private fun <T : Any> getNotNullConfig(config: JsonObject, key: String): T =
    requireNotNull(config.get<T>(key), { "postgres init need config of $key" })

  private suspend fun checkAndCreateDatabase(
    vertx: Vertx,
    database: String,
    host: String,
    port: Int,
    user: String,
    password: String
  ) {
    log.info("postgres init {} start, check: [{}]", ::checkAndCreateDatabase.name, database)

    val connectOptions =
      pgConnectOptionsOf(database = DEFAULT_DATABASE, host = host, port = port, user = user, password = password)
    val poolOptions = poolOptionsOf(maxSize = 1)
    val client = PgPool.pool(vertx, connectOptions, poolOptions)

    val res = client.preparedQueryAwait("""select datname from pg_database where datname = $1;""", Tuple.of(database))

    if (res.count() == 0) {
      // postgres message use english
      client.queryAwait("""create database $database encoding utf8 LC_COLLATE 'en_US.utf8' lc_ctype 'en_US.utf8' template template0;""")
    }

    client.close()

    log.info("postgres init {} end, check: [{}]", ::checkAndCreateDatabase.name, database)
  }

  private fun connectDatabase(
    vertx: Vertx,
    database: String,
    host: String,
    port: Int,
    user: String,
    password: String,
    poolSize: Int
  ) {
    val connectOptions =
      pgConnectOptionsOf(database = database, host = host, port = port, user = user, password = password)

    val poolOptions = poolOptionsOf(maxSize = poolSize)
    pool = PgPool.pool(vertx, connectOptions, poolOptions)

    log.info("{}, to: [{}], poolSize: {}", ::connectDatabase.name, database, poolSize)
  }

  private suspend fun PgPool.queryAwait(sql: String): RowSet<Row> {
    try {
      return awaitResult {
        this.query(sql).execute(it)
      }
    } catch (e: Throwable) {
      log.warn("error sql: {}, message: {}", sql, e.message)
      throw e
    }
  }

  private suspend fun PgPool.preparedQueryAwait(sql: String, tuple: Tuple): RowSet<Row> {
    try {
      return awaitResult {
        this.preparedQuery(sql).execute(tuple, it)
      }
    } catch (e: Throwable) {
      log.warn("error sql: {}, message: {}", sql, e.message)
      throw e
    }
  }

  // export
  suspend fun queryAwait(sql: String) = pool.queryAwait(sql)
  suspend fun preparedQueryAwait(sql: String, tuple: Tuple) = pool.preparedQueryAwait(sql, tuple)
  suspend fun preparedQueryAwait(sql: Sql<out Table>) = preparedQueryAwait(sql.get(), sql.tuple)

  suspend fun <T> safeSync(fn: suspend (Transaction) -> T): T {
    val transaction = pool.beginAwait()
    try {
      val res = fn(transaction)
      transaction.commitAwait()
      return res
    } catch (e: Throwable) {
      transaction.rollbackAwait()
      throw e
    }
  }

  suspend fun getNewUUID(): UUID = queryAwait("""select uuid_generate_v1();""").first().getUUID("uuid_generate_v1")

  fun longToLocalDateTime(ts: Long) = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())
  fun localDateTimeToLong(t: LocalDateTime?) =
    if (t == null) null else ZonedDateTime.of(t, ZoneId.systemDefault()).toInstant().toEpochMilli()

  fun currentLocalDateTime() = LocalDateTime.now()
  fun currentLongTime() = (currentLocalDateTime())!!
}
