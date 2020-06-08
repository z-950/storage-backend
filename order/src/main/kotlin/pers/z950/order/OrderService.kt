package pers.z950.order

import pers.z950.common.service.Close

interface OrderService : Close {
  suspend fun create(id: String, list: List<Pair<String, Int>>)
  suspend fun getAllNotChecked(worker: String): List<List<Order>>
  suspend fun getAllChecked(): List<Order>
  suspend fun checkOrder(uid: Int)
}
