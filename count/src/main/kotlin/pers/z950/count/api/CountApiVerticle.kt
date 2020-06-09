package pers.z950.count.api

import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import pers.z950.common.api.ApiVerticle
import pers.z950.common.api.Controller
import pers.z950.common.api.Success
import pers.z950.count.CountService

class CountApiVerticle(private val service: CountService) : ApiVerticle() {
  companion object {
    const val SERVICE_NAME = "count-rest-api"
  }

  override suspend fun start() {
    super.start()

    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    // cookie and session handler
    enableClusteredSession(router, getAuthProvider())

    dispatch(router)

    val host = config.getString("host", "0.0.0.0")
    val port = config.getInteger("port", 8994)
    createHttpServer(router, host, port)
    publishHttpEndpoint(SERVICE_NAME, host, port)
  }

  private fun dispatch(router: Router) {
    router.post("/").superHandler { create(it) }
    router.get("/all").superHandler { getAllCount(it) }
    router.get("/not-finished").superHandler { getNotFinished(it) }
    router.patch("/:id").superHandler { finish(it) }
  }

  @Controller
  private suspend fun create(ctx: RoutingContext) {
    val body = ctx.bodyAsJson
    val shelfId = body.getString("shelfId")
    val worker = body.getString("worker")

    service.create(shelfId, worker)

    @Success
    ctx.response(null)
  }

  @Controller
  private suspend fun getAllCount(ctx: RoutingContext) {
    @Success
    ctx.response(service.getAllCount())
  }

  @Controller
  private suspend fun getNotFinished(ctx: RoutingContext) {
    val worker = ctx.user().principal().getString("username")

    val res = service.getNotFinishedCount(worker)

    @Success
    ctx.response(res)
  }

  @Controller
  private suspend fun finish(ctx: RoutingContext) {
    val id = ctx.pathParam("id").toInt()

    service.finishCount(id)

    @Success
    ctx.response(null)
  }
}
