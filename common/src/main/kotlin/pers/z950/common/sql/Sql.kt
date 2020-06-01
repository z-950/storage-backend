package pers.z950.common.sql

import io.vertx.sqlclient.Tuple
import io.vertx.sqlclient.impl.ListTuple

// todo: handle null
class Sql<T : Table>(private val table: T) {
  private var sql: String = ""
  private var index: Int = 1
  val tuple: Tuple = ListTuple(mutableListOf())

  /**
   * Start with select sql.
   */
  fun select(): Sql<T> {
    sql = "select * from $table "
    return this
  }

  fun select(vararg columns: Column): Sql<T> {
    sql = "select ${columns.joinToString(",")} from $table "
    return this
  }

  fun count(vararg columns: Column): Sql<T> {
    sql += "select count(${columns.joinToString(",")}) from $table "
    return this
  }

  /**
   * Start with update sql.
   */
  fun update(): Sql<T> {
    sql = "update $table "
    return this
  }

  /**
   * Insert into
   */
  fun insert(vararg pairs: Pair<Column, Order>): Sql<T> {
    val fieldNames = pairs.joinToString(",") { "${it.first}" }
    val values = pairs.joinToString(",") { "${it.first} = $${index++}" }
    tuple.addValues(pairs.map { it.second }.toTypedArray())
    sql = "insert into $table ($fieldNames) values (${values}) "
    return this
  }

  /**
   * Set every field when update.
   */
  fun set(vararg pairs: Pair<Column, Order>): Sql<T> {
    tuple.addValues(pairs.map { it.second }.toTypedArray())
    val fieldNames = pairs.joinToString(",") { "${it.first} = $${index++}" }
    sql += "set $fieldNames "

    return this
  }

  /**
   * Where condition.
   * use e.g.
   * Sql(TABLE).select().where { with(it) { TABLE.id eq id } }
   */
  fun where(condition: (Sql<T>) -> Unit): Sql<T> {
    sql += "where "
    condition(this)
    return this
  }

  fun where(condition: String): Sql<T> {
    sql += "where $condition "
    return this
  }

  infix fun Column.eq(value: Any): Sql<T> {
    tuple.addValue(value)
    sql += "$this = $${index++} "
    return this@Sql
  }

  infix fun Column.like(value: Any): Sql<T> {
    tuple.addValue(value)
    sql += "$this like $${index++} "
    return this@Sql
  }

  /**
   * Where condition if not null.
   */
  fun whereIf(key: Column, value: Any?): Sql<T> {
    if (value != null) {
      tuple.addValue(value)
      sql += "where $key = $${index++} "
    }
    return this
  }

  /**
   * and
   */
  infix fun and(key: Column): Column {
    sql += "and "
    return key
  }

  /**
   * Or condition.
   */
  fun or(key: Column): Column {
    sql += "or "
    return key
  }

  data class Order(val value: String) {
    companion object {
      val ASC = Order("asc")
      val DESC = Order("desc")
    }

    override fun toString() = value
  }

  /**
   * order by [pairs]
   */
  fun orderBy(vararg pairs: Pair<Column, Order>): Sql<T> {
    sql += "order by ${pairs.joinToString(",") { "${it.first} ${it.second}" }} "
    return this
  }

  /**
   * page
   */
  fun page(limit: Int, offset: Int): Sql<T> {
    tuple.addValue(limit).addValue(offset)
    sql += "limit $${index++} offset $${index++} "
    return this
  }

  /**
   * Get sql.
   */
  fun get(): String = toString()
  override fun toString() = "$sql;"
}
