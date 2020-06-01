package pers.z950.gateway

import pers.z950.common.DeployableVerticle
import pers.z950.common.service.serviceFactory
import pers.z950.gateway.api.GatewayApiVerticle
import pers.z950.gateway.impl.AuthenticateServiceImpl
import io.vertx.core.DeploymentOptions
import io.vertx.kotlin.core.json.jsonObjectOf

class GatewayVerticle : DeployableVerticle() {
  override suspend fun start() {
    val authService = serviceFactory(
        AuthenticateServiceImpl::class.java,
        vertx,
        config.getJsonObject(SERVICE_CONFIG_KEY, jsonObjectOf())
    )

    deployVerticle(GatewayApiVerticle(authService), DeploymentOptions().setConfig(config.getJsonObject(API_CONFIG_KEY, jsonObjectOf())))
  }
}
