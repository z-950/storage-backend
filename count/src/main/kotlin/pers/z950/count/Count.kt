package pers.z950.count

data class Count(
  val id: Int,
  val shelfId: String,
  val worker: String,
  val finished: Boolean = false
)
