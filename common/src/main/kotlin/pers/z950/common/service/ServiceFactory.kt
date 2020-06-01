package pers.z950.common.service

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

suspend fun <T : AsyncInit> serviceFactory(serviceImpl: Class<T>, vertx: Vertx, config: JsonObject): T {
  return serviceImpl.getConstructor().newInstance().apply { init(vertx, config) }
}
