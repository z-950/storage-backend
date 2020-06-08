// code gen
package pers.z950.order

import com.fasterxml.jackson.core.type.TypeReference
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.eventbus.requestAwait
import io.vertx.kotlin.core.json.jsonObjectOf
import pers.z950.common.Mapper

class OrderServiceVertxEBProxy constructor(private val vertx: Vertx, private val address: String, private val options: DeliveryOptions) : OrderService {
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

  
  override suspend fun checkOrder(uid:Int):Unit {
    val jsonArgs = jsonObjectOf("uid" to uid)
    return getEventBusReplyValue("checkOrder", jsonArgs)
  }

  override suspend fun create(id:String, list:List<Pair<String,Int>>):Unit {
    val jsonArgs = jsonObjectOf("id" to id, "list" to list)
    return getEventBusReplyValue("create", jsonArgs)
  }

  override suspend fun getAllChecked():List<Order> {
    val jsonArgs = jsonObjectOf()
    return getEventBusReplyValue("getAllChecked", jsonArgs)
  }

  override suspend fun getAllNotChecked(worker:String):List<List<Order>> {
    val jsonArgs = jsonObjectOf("worker" to worker)
    return getEventBusReplyValue("getAllNotChecked", jsonArgs)
  }

}