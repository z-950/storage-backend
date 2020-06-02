package pers.z950.order

import pers.z950.common.service.Close

interface OrderService : Close {
  suspend fun create(map: Map<String, Int>): Order
  suspend fun getAllNotChecked(): List<Order>
  suspend fun checkOrder(id: Int, worker: String)
}
