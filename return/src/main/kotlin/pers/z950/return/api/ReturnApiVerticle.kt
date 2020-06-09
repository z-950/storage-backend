package pers.z950.`return`.api

import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import pers.z950.`return`.ReturnService
import pers.z950.common.api.ApiVerticle
import pers.z950.common.api.Controller
import pers.z950.common.api.Error
import pers.z950.common.api.Success

class ReturnApiVerticle(private val service: ReturnService) : ApiVerticle() {
  companion object {
    const val SERVICE_NAME = "return-rest-api"
  }

  override suspend fun start() {
    super.start()

    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    // cookie and session handler
    enableClusteredSession(router, getAuthProvider())

    dispatch(router)

    val host = config.getString("host", "0.0.0.0")
    val port = config.getInteger("port", 8995)
    createHttpServer(router, host, port)
    publishHttpEndpoint(SERVICE_NAME, host, port)
  }

  private fun dispatch(router: Router) {
    router.get("/not-checked").superHandler { get(it) }
    router.patch("/:uid").superHandler { check(it) }
  }

  @Controller
  private suspend fun get(ctx: RoutingContext) {
    @Success
    ctx.response(service.getAllNotChecked(ctx.user().principal().getString("username")))
  }

  @Controller
  private suspend fun check(ctx: RoutingContext) {
    @Error(400, "required")
    service.checkReturn(ctx.pathParam("uid").toInt(), ctx.user().principal().getString("username"))

    @Success
    ctx.response(null)
  }
}
