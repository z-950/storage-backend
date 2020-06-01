package pers.z950.common.api

import pers.z950.common.MicroServiceVerticle
import pers.z950.common.service.ServiceException
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.CookieSameSite
import io.vertx.core.json.DecodeException
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import io.vertx.ext.auth.shiro.ShiroAuth
import io.vertx.ext.auth.shiro.ShiroAuthOptions
import io.vertx.ext.auth.shiro.ShiroAuthRealmType
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.SessionHandler
import io.vertx.ext.web.sstore.ClusteredSessionStore
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.core.json.jsonObjectOf
import kotlinx.coroutines.launch
import org.apache.shiro.authc.IncorrectCredentialsException
import org.apache.shiro.authc.UnknownAccountException

abstract class ApiVerticle : MicroServiceVerticle() {
  companion object {
    const val DEFAULT_SESSION_TIMEOUT = 3 * 24 * 3_600_000L
  }

  /**
   * Create http server for the REST service.
   */
  suspend fun createHttpServer(router: Router, host: String, port: Int) {
    vertx.createHttpServer().requestHandler(router).listenAwait(port, host)
    router.errorHandler(ApiException.NOT_FOUND.code) { notFoundHandler(it) }
    router.errorHandler(ApiException.ERROR.code) { exceptionHandler(it, it.failure()) }

    log.info("create http server at: [$host:$port]")
  }

  /**
   * use shiro auth, properties realm.
   * rules in shrio-auth.properties
   */
  fun getAuthProvider(): AuthProvider {
    return ShiroAuth.create(
      vertx,
      ShiroAuthOptions().setType(ShiroAuthRealmType.PROPERTIES)
        .setConfig(jsonObjectOf("properties_path" to "classpath:shrio-auth.properties"))
    )
  }

  /**
   * in vert.x 3.x, [User] cache permission, clear cache for update permission
   * use in authorize, after get [user]
   * if not, [user] store in session with old permission, for reducing permissions, should restart all services (the backend)
   */
  fun hackUpdatePermission(user: User?) {
    user?.clearCache()
  }

  /**
   * Enable clustered session storage in requests.
   */
  fun enableClusteredSession(router: Router, authProvider: AuthProvider? = null) {
    router.route().handler(
      SessionHandler.create(
        ClusteredSessionStore.create(vertx, "gateway.user.session")
      ).apply {
        setCookieHttpOnlyFlag(true)
        setCookieSameSite(CookieSameSite.STRICT)
        setSessionTimeout(
          config.getLong(
            "session.timeout",
            DEFAULT_SESSION_TIMEOUT
          )
        )
        if (authProvider != null) {
          setAuthProvider(authProvider)
        }
      }
    )
  }

  fun notFoundHandler(ctx: RoutingContext) {
    if (!ctx.response().ended()) {
      val e = ApiException.NOT_FOUND
      ctx.error(e)
      logWarn(ctx, e)
    }
  }

  open fun exceptionHandler(ctx: RoutingContext, e: Throwable) {
    if (!ctx.response().ended()) {
      when (e) {
        is NullPointerException, is ClassCastException, is IllegalArgumentException, is NumberFormatException, is DecodeException -> {
          // data parse error, e.g. parse json with a `null` key,type not match, illegal uuid string, illegal number
          ctx.error(ApiException.ILLEGAL_ARGS)
          logWarn(ctx, e)
        }
        is io.vertx.serviceproxy.ServiceException -> {
          // service error (by proxy)
          when (e.failureCode()) {
            ServiceException.ILLEGAL_ARGS.code, ServiceException.WITHOUT_REQUEST.code -> {
              ctx.error(ApiException.ILLEGAL_ARGS)
            }
            ServiceException.NOT_FOUND.code -> {
              ctx.error(ApiException.NOT_FOUND)
            }
            else -> {
              ctx.error(ApiException.ERROR)
            }
          }
          logWarn(ctx, e)
        }
        is ServiceException -> {
          // service error (throw directly)
          when (e.code) {
            ServiceException.ILLEGAL_ARGS.code, ServiceException.WITHOUT_REQUEST.code -> {
              ctx.error(ApiException.ILLEGAL_ARGS)
              logWarn(ctx, e)
            }
            ServiceException.NOT_FOUND.code -> {
              ctx.error(ApiException.NOT_FOUND)
              logWarn(ctx, e)
            }
            else -> {
              ctx.error(ApiException.ERROR)
              logError(ctx, e)
            }
          }
        }
        is ApiException -> {
          // api error
          if (e.code != ApiException.ERROR.code) {
            ctx.error(e)
            logWarn(ctx, e)
          } else {
            ctx.error(ApiException.ERROR)
            logError(ctx, e)
          }
        }
        is IncorrectCredentialsException, is UnknownAccountException -> {
          ctx.error(ApiException.WRONG_DATA)
          log.info(ctx, ApiException.WRONG_DATA.message)
        }
        else -> {
          // unknown error
          ctx.error(ApiException.ERROR)
          logError(ctx, e)
        }
      }
    }
  }

  open fun RoutingContext.error(e: ApiException) {
    response().setStatusCode(e.code).end(e.message)
  }

  open fun logWarn(ctx: RoutingContext, e: Throwable) {
    log.warn(
      "{} {} from {}, {}",
      ctx.request().method().name,
      ctx.normalisedPath(),
      ctx.request().connection().remoteAddress().host(),
      e.message
    )
  }

  open fun logError(ctx: RoutingContext, e: Throwable) {
    log.error(
      "{} {} from {} {}",
      ctx.request().method().name,
      ctx.normalisedPath(),
      ctx.request().connection().remoteAddress().host(),
      e.message,
      e
    )
  }

  /**
   * call with [exceptionHandler] implement
   */
  fun Route.superHandler(suspendHandler: suspend (RoutingContext) -> Unit) {
    handler { ctx ->
      launch {
        try {
          suspendHandler(ctx)
        } catch (e: Throwable) {
          exceptionHandler(ctx, e)
        }
      }
    }
  }

  /**
   * response json value
   */
  fun <T> RoutingContext.response(value: T) = response().setStatusCode(HttpResponseStatus.OK.code())
    .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
    .end(jsonStringify(value))

  private fun <T> jsonStringify(value: T) = DatabindCodec.mapper().writeValueAsString(value)
}
