package pers.z950.order.impl

import pers.z950.common.service.repository.PostgresRepositoryWrapper
import io.vertx.serviceproxy.ServiceProxyBuilder
import pers.z950.order.Order
import pers.z950.order.OrderService
import pers.z950.product.ProductService
import java.util.*

class OrderServiceImpl : PostgresRepositoryWrapper(), OrderService {
  override suspend fun create(map: Map<String, Int>): Order {
    val service = ServiceProxyBuilder(vertx).setAddress("service.product").build(ProductService::class.java)

    map.forEach {
      //todo: check map
      val res = service.getProduct(it.key)
      log.info("order get res: {}", res)
    }
    return Order(UUID.randomUUID().toString(), map)
  }
}
