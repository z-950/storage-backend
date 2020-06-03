package pers.z950.product.api

import pers.z950.common.api.ApiVerticle
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import pers.z950.common.api.Controller
import pers.z950.common.api.Error
import pers.z950.common.api.Success
import pers.z950.product.ProductService

class ProductApiVerticle(private val service: ProductService) : ApiVerticle() {
  companion object {
    const val SERVICE_NAME = "product-rest-api"
  }

  override suspend fun start() {
    super.start()

    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())

    dispatch(router)

    val host = config.getString("host", "0.0.0.0")
    val port = config.getInteger("port", 8991)
    createHttpServer(router, host, port)
    publishHttpEndpoint(SERVICE_NAME, host, port)
  }

  override suspend fun stop() {
    service.close()
    super.stop()
  }

  private fun dispatch(router: Router) {
    router.get("/:productId").superHandler { get(it) }
  }

  @Controller
  private suspend fun get(ctx: RoutingContext) {
    @Error(400, "required")
    val productId = ctx.pathParam("productId")
    val product = service.getProduct(productId)

    @Success
    ctx.response(product)
  }
}
