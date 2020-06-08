package pers.z950.order

import pers.z950.common.DeployableVerticle
import pers.z950.common.service.serviceFactory
import io.vertx.core.DeploymentOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import pers.z950.order.api.OrderApiVerticle
import pers.z950.order.impl.OrderServiceImpl
import pers.z950.product.ProductService

class OrderVerticle : DeployableVerticle() {
  override suspend fun start() {
    val orderService = serviceFactory(
        OrderServiceImpl::class.java,
        vertx,
        config.getJsonObject(SERVICE_CONFIG_KEY, jsonObjectOf())
    )

    proxyHelper(OrderService::class.java, orderService)

    deployVerticle(OrderApiVerticle(orderService), DeploymentOptions().setConfig(config.getJsonObject(API_CONFIG_KEY, jsonObjectOf())))
  }
}
