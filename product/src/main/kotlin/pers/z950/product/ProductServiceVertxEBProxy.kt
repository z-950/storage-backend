// code gen
package pers.z950.product

import com.fasterxml.jackson.core.type.TypeReference
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.kotlin.core.eventbus.requestAwait
import io.vertx.kotlin.core.json.jsonObjectOf

class ProductServiceVertxEBProxy constructor(private val vertx: Vertx, private val address: String, private val options: DeliveryOptions) : ProductService {
  private var closed: Boolean = false

  constructor(vertx: Vertx, address: String) : this(vertx, address, DeliveryOptions())

  override fun close() {
    closed = true
  }

  private inline fun <reified T> unwrap(res: String): T = DatabindCodec.mapper().readValue(res, object : TypeReference<T>() {})

  private suspend inline fun <reified T> getEventBusReplyValue(action: String, jsonArgs: JsonObject): T {
    if (closed) {
      throw (IllegalStateException("Proxy is closed"))
    }

    val deliveryOptions = options
    deliveryOptions.addHeader("action", action)
    val message = vertx.eventBus().requestAwait<String>(address, jsonArgs, deliveryOptions)
    return unwrap(message.body())
  }

  
  override suspend fun getProduct(id:String):Product {
    val jsonArgs = jsonObjectOf("id" to id)
    return getEventBusReplyValue("getProduct", jsonArgs)
  }

}