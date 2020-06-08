package pers.z950.order

data class Order(
  val uid: Int, // increase id
  val id: String, // order id
  val product: String,
  val number: Int,
  val isChecked: Boolean = false,
  val checker: String? = null
)
