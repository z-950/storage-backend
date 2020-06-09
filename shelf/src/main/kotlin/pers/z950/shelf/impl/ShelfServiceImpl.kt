package pers.z950.shelf.impl

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.serviceproxy.ServiceProxyBuilder
import pers.z950.common.service.AsyncInit
import pers.z950.common.service.repository.PostgresRepositoryWrapper
import pers.z950.product.Product
import pers.z950.product.ProductService
import pers.z950.shelf.ShelfService

class ShelfServiceImpl : AsyncInit, ShelfService {
  override suspend fun init(vertx: Vertx, config: JsonObject) {}

  override fun getShelfList(): List<String> {
    return hackGetShelfList()
  }

  private fun hackGetShelfList() = listOf("A", "B", "C", "D")
}
