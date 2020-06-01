package pers.z950.common.service

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

interface AsyncInit {
  suspend fun init(vertx: Vertx, config: JsonObject)
}
