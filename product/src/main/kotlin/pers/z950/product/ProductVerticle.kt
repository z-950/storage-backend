package pers.z950.product

import pers.z950.common.DeployableVerticle
import pers.z950.common.service.serviceFactory
import io.vertx.core.DeploymentOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import pers.z950.product.api.ProductApiVerticle
import pers.z950.product.impl.ProductServiceImpl

class ProductVerticle : DeployableVerticle() {
  override suspend fun start() {
    val productService = serviceFactory(
        ProductServiceImpl::class.java,
        vertx,
        config.getJsonObject(SERVICE_CONFIG_KEY, jsonObjectOf())
    )

    proxyHelper(ProductService::class.java, productService)

    deployVerticle(ProductApiVerticle(productService), DeploymentOptions().setConfig(config.getJsonObject(API_CONFIG_KEY, jsonObjectOf())))
  }
}
