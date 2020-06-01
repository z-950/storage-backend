package pers.z950.common.service

import io.vertx.core.json.JsonObject

/**
 * service exception
 */
data class ServiceException(val code: Int, override val message: String) : Throwable("${ServiceException::class.java.name}: $message") {
  constructor(json: JsonObject) : this(code = json.getInteger("code"), message = json.getString("message"))

  private object CODE {
    const val ILLEGAL_ARGS = 400
    const val WITHOUT_REQUEST = 401
    const val NOT_FOUND = 404
    const val ERROR = 500
  }

  companion object {
    @JvmStatic
    val NOT_FOUND = ServiceException(
        CODE.NOT_FOUND,
        "no service"
    )

    @JvmField
    val ILLEGAL_ARGS = ServiceException(
        CODE.ILLEGAL_ARGS,
        "illegal arguments"
    )

    @JvmField
    val WITHOUT_REQUEST = ServiceException(
        CODE.WITHOUT_REQUEST,
        "without request"
    )

    @JvmField
    val ERROR = ServiceException(
        CODE.ERROR,
        "server error"
    )

    @JvmStatic
    fun error(message: String) = ServiceException(
        ERROR.code,
        message
    )
  }
}
