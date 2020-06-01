package pers.z950.order.api

import pers.z950.common.api.ApiException
import pers.z950.common.api.ApiVerticle
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.awaitResult
import pers.z950.order.Order
import pers.z950.order.OrderService

class OrderApiVerticle(private val service: OrderService) : ApiVerticle() {
  companion object {
    const val SERVICE_NAME = "order-rest-api"
    const val SERVICE_NAME_IN_AUTH = "pers/z950/order"
  }

  private lateinit var shiroAuth: AuthProvider

  override suspend fun start() {
    super.start()

    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    shiroAuth = getAuthProvider()
    // cookie and session handler
    enableClusteredSession(router, shiroAuth)

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
    router.post("/").superHandler { post(it) }
  }

  // for auth
  // authorize, use in dispatch
  private suspend fun authorize(ctx: RoutingContext, level: String) {
    val method = ctx.request().method().name.toLowerCase()
    val user = ctx.user()
    hackUpdatePermission(user)

    val has = awaitResult<Boolean> { user.isAuthorised("$SERVICE_NAME_IN_AUTH:$method:$level", it) }

    if (!has) {
      throw ApiException.FORBIDDEN
    }
  }

  private suspend fun post(ctx: RoutingContext) {
    authorize(ctx, "self")

    val map = ctx.bodyAsJson.map.mapValues { it.value as Int }

    log.info("receive: {}", map)

//    val res = service.create(map)
    val res = Order("iiiiiid", mapOf("abc" to 1))

    ctx.response(res)
  }
}
