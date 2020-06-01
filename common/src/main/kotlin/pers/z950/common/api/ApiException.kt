package pers.z950.common.api

import io.netty.handler.codec.http.HttpResponseStatus

/**
 * response [code] and [message].
 * [code] come from http status.
 */
data class ApiException(val code: Int, override val message: String) : Throwable("${ApiException::class.java.name}: $message") {
  constructor(status: HttpResponseStatus) : this(status.code(), status.reasonPhrase())

  companion object {
    val ILLEGAL_ARGS get() = ApiException(HttpResponseStatus.BAD_REQUEST)

    val WRONG_DATA get() = ApiException(HttpResponseStatus.UNPROCESSABLE_ENTITY)

    val NOT_AUTHENTICATION get() = ApiException(HttpResponseStatus.NETWORK_AUTHENTICATION_REQUIRED)

    val FORBIDDEN get() = ApiException(HttpResponseStatus.FORBIDDEN)

    val NOT_FOUND get() = ApiException(HttpResponseStatus.NOT_FOUND)

    val ERROR get() = ApiException(HttpResponseStatus.INTERNAL_SERVER_ERROR)

    val TOO_LARGE get() = ApiException(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE)

    val WRONG_TYPE get() = ApiException(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE)

    @JvmStatic
    fun forbidden(message: String) =
        ApiException(
            FORBIDDEN.code,
            message
        )
  }
}
