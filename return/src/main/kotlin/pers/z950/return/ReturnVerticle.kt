package pers.z950.`return`

import io.vertx.core.DeploymentOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import pers.z950.`return`.api.ReturnApiVerticle
import pers.z950.`return`.impl.ReturnServiceImpl
import pers.z950.common.DeployableVerticle
import pers.z950.common.service.serviceFactory

class ReturnVerticle : DeployableVerticle() {
  override suspend fun start() {
    val returnService = serviceFactory(
      ReturnServiceImpl::class.java,
      vertx,
      config.getJsonObject(SERVICE_CONFIG_KEY, jsonObjectOf())
    )

    deployVerticle(ReturnApiVerticle(returnService), DeploymentOptions().setConfig(config.getJsonObject(API_CONFIG_KEY, jsonObjectOf())))
  }
}
