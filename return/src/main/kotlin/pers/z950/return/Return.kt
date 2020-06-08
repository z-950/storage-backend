package pers.z950.`return`

data class Return(
  val uid: Int,
  val orderId: String,
  val productId: String,
  val number: Int,
  val isChecked: Boolean = false,
  val checker: String? = null
)
