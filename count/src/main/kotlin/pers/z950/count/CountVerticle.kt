package pers.z950.count

import io.vertx.core.DeploymentOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import pers.z950.common.DeployableVerticle
import pers.z950.common.service.serviceFactory
import pers.z950.count.api.CountApiVerticle
import pers.z950.count.impl.CountServiceImpl

class CountVerticle : DeployableVerticle() {
  override suspend fun start() {
    val countService = serviceFactory(
      CountServiceImpl::class.java,
      vertx,
      config.getJsonObject(SERVICE_CONFIG_KEY, jsonObjectOf())
    )

    deployVerticle(
      CountApiVerticle(countService),
      DeploymentOptions().setConfig(config.getJsonObject(API_CONFIG_KEY, jsonObjectOf()))
    )
  }
}
