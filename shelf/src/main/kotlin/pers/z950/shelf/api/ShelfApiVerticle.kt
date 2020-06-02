package pers.z950.shelf.api

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.serviceproxy.ServiceProxyBuilder
import pers.z950.common.api.ApiVerticle
import pers.z950.common.api.Controller
import pers.z950.common.api.Success
import pers.z950.product.Product
import pers.z950.product.ProductService

class ShelfApiVerticle : ApiVerticle() {
  companion object {
    const val SERVICE_NAME = "shelf-rest-api"
  }

  override suspend fun start() {
    super.start()

    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())

    dispatch(router)

    val host = config.getString("host", "0.0.0.0")
    val port = config.getInteger("port", 8993)
    createHttpServer(router, host, port)
    publishHttpEndpoint(SERVICE_NAME, host, port)
  }

  private fun dispatch(router: Router) {
    router.get("/list").superHandler { getShelfList(it) }
    router.patch("/:id").superHandler { updateShelf(it) }
  }

  @Controller
  private fun getShelfList(ctx: RoutingContext) {
    @Success
    ctx.response(hackGetShelfList())
  }

  @Controller
  private suspend fun updateShelf(ctx: RoutingContext) {
    val body = ctx.bodyAsJsonArray

    hackUpdateShelf(body.map { (it as JsonObject).mapTo(Product::class.java) })

    @Success
    ctx.response(null)
  }

  private fun hackGetShelfList() = listOf("A", "B", "C", "D")

  private suspend fun hackUpdateShelf(list:List<Product>){
    val productService = ServiceProxyBuilder(vertx).setAddress("service.product").build(ProductService::class.java)

    productService.updateProducts(list)
  }
}
