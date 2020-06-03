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
  private lateinit var productService: ProductService

  override suspend fun init(vertx: Vertx, config: JsonObject) {
    productService = ServiceProxyBuilder(vertx).setAddress("service.product").build(ProductService::class.java)
  }

  override fun getShelfList(): List<String> {
    return hackGetShelfList()
  }

  override suspend fun updateShelf(list: List<Product>) {
    productService.updateProducts(list)
  }

  private fun hackGetShelfList() = listOf("A", "B", "C", "D")
}
