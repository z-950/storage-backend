package pers.z950.gateway.api

import pers.z950.common.api.*
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.circuitbreaker.CircuitBreakerOptions
import io.vertx.core.Promise
import io.vertx.core.http.HttpClient
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.kotlin.circuitbreaker.executeAwait
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.ext.auth.authenticateAwait
import io.vertx.kotlin.ext.web.client.sendAwait
import io.vertx.kotlin.ext.web.client.sendBufferAwait
import io.vertx.kotlin.servicediscovery.getRecordsAwait
import io.vertx.servicediscovery.ServiceDiscovery
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class GatewayApiVerticle() : ApiVerticle() {
  companion object {
    const val PROXY_PATH = "/b"
    const val DEFAULT_TIMEOUT = 10_000L

    const val DEFAULT_ROLE = "tourist"
  }

  private lateinit var circuitBreaker: CircuitBreaker
  private lateinit var shiroAuth: AuthProvider

  override suspend fun start() {
    super.start()

    // init circuit breaker instance
    val cbOptions = config.getJsonObject("circuit-breaker") ?: JsonObject()
    circuitBreaker = CircuitBreaker.create(
      cbOptions.getString("name", "circuit-breaker"),
      vertx,
      CircuitBreakerOptions()
        .setMaxFailures(cbOptions.getInteger("max-failures", 5))
        .setTimeout(
          cbOptions.getLong(
            "timeout",
            DEFAULT_TIMEOUT
          )
        )
        .setResetTimeout(cbOptions.getLong("reset-timeout", 30_000L))
    )

    createHttpServer(getRootRouter(), config.getString("host", "localhost"), config.getInteger("port", 8888))
  }

  override suspend fun stop() {
    circuitBreaker.close()
    super.stop()
  }

  private suspend fun getRootRouter(): Router {
    val rootRouter = Router.router(vertx)

    // api proxy
    rootRouter.mountSubRouter(PROXY_PATH, getApiRouter())

    // static content (for local)
    rootRouter.route("/*").handler(StaticHandler.create())

    return rootRouter
  }

  private suspend fun getApiRouter(): Router {
    val apiRouter = Router.router(vertx)

    // notice: not auto reload properties
    shiroAuth = getAuthProvider()

    // body handler todo: upload file?
    apiRouter.route().handler(BodyHandler.create())
    // cookie and session handler
    enableClusteredSession(apiRouter, shiroAuth)

    // auth
    authDispatch(apiRouter)

    // api dispatch
    apiRouter.route().superHandler { dispatchRequest(it) }

    return apiRouter
  }

  private suspend fun authDispatch(router: Router) {
    val authPath = "/session"
    router.post(authPath).superHandler { authenticate(it) }
    router.get(authPath).superHandler { info(it) }
    router.delete(authPath).superHandler { logout(it) }
  }

  @Controller
  private suspend fun dispatchRequest(ctx: RoutingContext) {
    // todo: 优化为一个任务调度（当前为两次）
    circuitBreaker.executeAwait<Unit> {
      launch {
        withTimeoutOrNull(DEFAULT_TIMEOUT) {
          try {
            val recordList = getAllEndpoints()
            // get relative path and retrieve prefix to dispatch client
            val path = ctx.request().uri().replace("$PROXY_PATH/", "")
            val service = path.split("/").first()

            @Error(403, "no permission")
            authorize(ctx, service)

            // get one relevant HTTP client, may not exist
            val client = recordList.firstOrNull { it.name == "$service-rest-api" }

            if (client != null) {
              val httpClient = discovery.getReference(client).get<HttpClient>()
              @Error(500 - 599, "see inside") @Success("see inside")
              reverseProxy(ctx, path, httpClient, it)
            } else {
              @Error(404, "service not found")
              notFoundHandler(ctx)
              it.complete()
            }
          } catch (e: Throwable) {
            @Error(500, "unknown error, throw by excuteAwait")
            log.error(e)
            it.fail(e)
          }
        }
      }
    }
  }

  @Controller
  private suspend fun reverseProxy(ctx: RoutingContext, path: String, client: HttpClient, promise: Promise<Unit>) {
    val realClient = WebClient.wrap(client, WebClientOptions().setFollowRedirects(false))
    val req = realClient.request(ctx.request().method(), path).apply {
      // set headers
      putHeaders(ctx.request().headers())
      if (ctx.user() != null) putHeader("user-principal", ctx.user().principal().encode())
    }

    // send
    val res = if (ctx.body == null || ctx.body.length() == 0) {
      req.sendAwait()
    } else {
      req.sendBufferAwait(ctx.body)
    }

    // handle result
    val statusCode = res.statusCode()
    if (statusCode >= 500) {
      // api endpoint server error, circuit breaker should fail
      // error will be throw by `executeAwait`
      @Error(500 - 599, "service error")
      promise.fail(ApiException(statusCode, res.bodyAsString()))
    } else {
      @Success("include 1xx/2xx/3xx/4xx status")
      ctx.response().apply {
        setStatusCode(statusCode)
        headers().addAll(res.headers())
        end(res.body())
      }
      promise.complete()
    }

    ServiceDiscovery.releaseServiceObject(discovery, client)
  }

  // for gateway
  private suspend fun getAllEndpoints() =
    discovery.getRecordsAwait { it.type == io.vertx.servicediscovery.types.HttpEndpoint.TYPE }

  @Controller
  private suspend fun authenticate(ctx: RoutingContext) {
    val body = ctx.bodyAsJson
    val username: String? = body.getString("username")
    val password: String? = body.getString("password")

    val user = shiroAuth.authenticateAwait(jsonObjectOf("username" to username, "password" to password))

    ctx.setUser(user)
    val role = hackGetRole()

    @Success("user info")
    ctx.response(jsonObjectOf("id" to username, "role" to role))
  }

  @Controller
  private fun info(ctx: RoutingContext) {
    // username is role
    val role = ctx.user()?.principal()?.getString("username") ?: DEFAULT_ROLE

    @Success("user info")
    ctx.response(jsonObjectOf("role" to role, "id" to ctx.session().get<String>("id")))
  }

  @Controller
  private fun logout(ctx: RoutingContext) {
    ctx.clearUser()
    ctx.session().destroy()

    @Success
    ctx.response(null)
  }

  // for auth
  // authorize, use in dispatch
  private suspend fun authorize(ctx: RoutingContext, service: String) {
    val method = ctx.request().method().name.toLowerCase()
    var user: User? = ctx.user()

    if (user == null) {
      user = hackShiroAuthnForAuthz(DEFAULT_ROLE)
      ctx.setUser(user)
    }
    hackUpdatePermission(user)

    val has = awaitResult<Boolean> { user.isAuthorised("$service:$method", it) }

    if (!has) {
      throw ApiException.FORBIDDEN
    }
  }

  // hack for shiroAuth authorize. username & password are role name, for tourist
  private suspend fun hackShiroAuthnForAuthz(role: String): User {
    return shiroAuth.authenticateAwait(jsonObjectOf("username" to role, "password" to role))
  }

  // hack for get user role (worker only)
  private fun hackGetRole() = "worker"
}
