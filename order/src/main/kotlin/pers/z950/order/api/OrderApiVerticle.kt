package pers.z950.order.api

import io.vertx.core.json.JsonObject
import pers.z950.common.api.ApiVerticle
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import pers.z950.common.api.Controller
import pers.z950.common.api.Error
import pers.z950.common.api.Success
import pers.z950.order.OrderService

class OrderApiVerticle(private val service: OrderService) : ApiVerticle() {
  companion object {
    const val SERVICE_NAME = "order-rest-api"
  }

  override suspend fun start() {
    super.start()

    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    // cookie and session handler
    enableClusteredSession(router, getAuthProvider())

    dispatch(router)

    val host = config.getString("host", "0.0.0.0")
    val port = config.getInteger("port", 8992)
    createHttpServer(router, host, port)
    publishHttpEndpoint(SERVICE_NAME, host, port)
  }

  override suspend fun stop() {
    service.close()
    super.stop()
  }

  private fun dispatch(router: Router) {
    router.get("/not-checked").superHandler { getAllNotChecked(it) }
    router.patch("/:orderUid").superHandler { checkOrder(it) }
    router.post("/create").superHandler { createOrder(it) }
  }

  @Controller
  private suspend fun getAllNotChecked(ctx: RoutingContext) {
    val worker = ctx.user().principal().getString("username")

    val res = service.getAllNotChecked(worker)

    @Success
    ctx.response(res)
  }

  @Controller
  private suspend fun checkOrder(ctx: RoutingContext) {
    @Error(400, "required")
    val orderUid = ctx.pathParam("orderUid").toInt()

    service.checkOrder(orderUid)

    @Success
    ctx.response(null)
  }

  @Controller
  private suspend fun createOrder(ctx: RoutingContext) {
    @Error(400, "required")
    val body = ctx.bodyAsJson
    val id = body.getString("id")
    val list = body.getJsonArray("list")
      .map { (it as JsonObject).map.entries.first().toPair() as Pair<String, Int> }

    val order = service.create(id, list)

    @Success
    ctx.response(order)
  }
}
