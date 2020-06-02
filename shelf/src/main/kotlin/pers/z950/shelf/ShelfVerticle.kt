package pers.z950.shelf

import io.vertx.core.DeploymentOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import pers.z950.common.DeployableVerticle
import pers.z950.shelf.api.ShelfApiVerticle

class ShelfVerticle : DeployableVerticle() {
  override suspend fun start() {
    deployVerticle(
      ShelfApiVerticle(),
      DeploymentOptions().setConfig(config.getJsonObject(API_CONFIG_KEY, jsonObjectOf()))
    )
  }
}
