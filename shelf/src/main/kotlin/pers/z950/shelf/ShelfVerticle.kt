package pers.z950.shelf

import io.vertx.core.DeploymentOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import pers.z950.common.DeployableVerticle
import pers.z950.common.service.serviceFactory
import pers.z950.shelf.api.ShelfApiVerticle
import pers.z950.shelf.impl.ShelfServiceImpl

class ShelfVerticle : DeployableVerticle() {
  override suspend fun start() {
    val service = serviceFactory(ShelfServiceImpl::class.java, vertx, config.getJsonObject(SERVICE_CONFIG_KEY))

    deployVerticle(
      ShelfApiVerticle(service),
      DeploymentOptions().setConfig(config.getJsonObject(API_CONFIG_KEY, jsonObjectOf()))
    )
  }
}
