package pers.z950.order

data class Order(
  val id: Int,
  val productList: List<String>,
  val numberList: List<Int>,
  val isChecked: Boolean = false,
  val checker: String? = null
)
