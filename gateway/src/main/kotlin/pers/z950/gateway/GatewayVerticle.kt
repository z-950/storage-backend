package pers.z950.gateway

import pers.z950.common.DeployableVerticle
import pers.z950.gateway.api.GatewayApiVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.kotlin.core.json.jsonObjectOf

class GatewayVerticle : DeployableVerticle() {
  override suspend fun start() {
    deployVerticle(GatewayApiVerticle(), DeploymentOptions().setConfig(config.getJsonObject(API_CONFIG_KEY, jsonObjectOf())))
  }
}
