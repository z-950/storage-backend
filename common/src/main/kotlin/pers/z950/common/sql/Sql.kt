package pers.z950.common.sql

import io.vertx.sqlclient.Tuple
import io.vertx.sqlclient.impl.ListTuple

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

  fun count(): Sql<T> {
    sql += "select count(*) from $table "
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
   * if value is List, call toTypedArray
   */
  fun insert(vararg pairs: Pair<Column, Any>): Sql<T> {
    val fieldNames = pairs.joinToString(",") { "${it.first}" }
    val values = pairs.joinToString(",") { "$${index++}" }
    pairs.forEach { (_, value) ->
      if (value is Array<*>) {
        tuple.addValues(value)
      } else {
        tuple.addValue(value)
      }
    }
    sql = "insert into $table ($fieldNames) values (${values}) "
    return this
  }

  fun returning(column: Column): Sql<T> {
    sql += "returning $column "
    return this
  }

  fun onConflictDoNoting(column: Column): Sql<T> {
    sql += "on conflict($column) do nothing "
    return this
  }

  fun forUpdate(): Sql<T> {
    sql += "for update "
    return this
  }

  /**
   * Set every field when update.
   */
  fun set(vararg pairs: Pair<Column, Any>): Sql<T> {
    pairs.forEach { (_, value) ->
      if (value is Array<*>) {
        tuple.addValues(value)
      } else {
        tuple.addValue(value)
      }
    }
    val fieldNames = pairs.joinToString(",") { "${it.first} = $${index++}" }
    sql += "set $fieldNames "

    return this
  }

  fun set(vararg strings: String): Sql<T> {
    sql += "set ${strings.joinToString(",")} "
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

  infix fun Column.eq(value: Any): Column {
    tuple.addValue(value)
    sql += "$this = $${index++} "
    return this
  }

  infix fun Column.like(value: Any): Column {
    tuple.addValue(value)
    sql += "$this like $${index++} "
    return this
  }

  fun Column.isNull(): String {
    return "$this is null "
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
  infix fun Column.and(key: Column): Column {
    sql += "and "
    return key
  }

  infix fun Column.and(str: String) {
    sql += "and $str"
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
   * operator
   */
  infix fun Column.minus(value: Any): String {
    tuple.addValue(value)
    return "$this - $${index++}"
  }

  infix fun Column.plus(value: Any): String {
    tuple.addValue(value)
    return "$this + $${index++}"
  }

  /**
   * Get sql.
   */
  fun get(): String = toString()
  override fun toString() = "$sql;"
}
