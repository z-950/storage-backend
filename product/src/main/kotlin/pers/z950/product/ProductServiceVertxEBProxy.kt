// code gen
package pers.z950.product

import com.fasterxml.jackson.core.type.TypeReference
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.eventbus.requestAwait
import io.vertx.kotlin.core.json.jsonObjectOf
import pers.z950.common.Mapper

class ProductServiceVertxEBProxy constructor(private val vertx: Vertx, private val address: String, private val options: DeliveryOptions) : ProductService {
  private var closed: Boolean = false

  constructor(vertx: Vertx, address: String) : this(vertx, address, DeliveryOptions())

  override fun close() {
    closed = true
  }

  private inline fun <reified T> unwrap(res: String): T = Mapper.jackson.readValue(res, object : TypeReference<T>() {})

  private suspend inline fun <reified T> getEventBusReplyValue(action: String, jsonArgs: JsonObject): T {
    if (closed) {
      throw (IllegalStateException("Proxy is closed"))
    }

    val deliveryOptions = options
    deliveryOptions.addHeader("action", action)
    val message = vertx.eventBus().requestAwait<String>(address, jsonArgs, deliveryOptions)
    return unwrap(message.body())
  }

  
  override suspend fun getAllProduct():List<Product> {
    val jsonArgs = jsonObjectOf()
    return getEventBusReplyValue("getAllProduct", jsonArgs)
  }

  override suspend fun getProduct(id:String):Product {
    val jsonArgs = jsonObjectOf("id" to id)
    return getEventBusReplyValue("getProduct", jsonArgs)
  }

  override suspend fun reduceProducts(map:Map<String,Int>):Unit {
    val jsonArgs = jsonObjectOf("map" to map)
    return getEventBusReplyValue("reduceProducts", jsonArgs)
  }

  override suspend fun updateProducts(list:List<Product>):Unit {
    val jsonArgs = jsonObjectOf("list" to list)
    return getEventBusReplyValue("updateProducts", jsonArgs)
  }

}