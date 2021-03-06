// code gen
package pers.z950.order

import pers.z950.common.service.ServiceException
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.serviceproxy.ProxyHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pers.z950.common.Mapper

class OrderServiceVertxProxyHandler(private val vertx: Vertx, private val service: OrderService, topLevel: Boolean = true, private val timeoutSeconds: Long = 300) : ProxyHandler() {
  private val log = LoggerFactory.getLogger(OrderServiceVertxProxyHandler::javaClass.name)

  private var timerID: Long = -1L
  private var lastAccessed: Long = 0

  init {
    if (timeoutSeconds != -1L && !topLevel) {
      var period = timeoutSeconds * 1000 / 2
      if (period > 10000) {
        period = 10000
      }
      this.timerID = vertx.setPeriodic(period) { this.checkTimedOut(it) }
    } else {
      this.timerID = -1
    }
    accessed()
  }

  private fun checkTimedOut(id: Long) {
    val now = System.nanoTime()
    if (now - lastAccessed > timeoutSeconds * 1000000000) {
      close()
    }
  }

  override fun close() {
    if (timerID != -1L) {
      vertx.cancelTimer(timerID)
    }
    super.close()
  }

  private fun accessed() {
    this.lastAccessed = System.nanoTime()
  }

  private fun failedHandler(msg: Message<JsonObject>, t: Throwable) {
    if (t is ServiceException) {
      msg.fail(t.code, t.message)
    } else {
      msg.fail(500, t.message)
    }
    log.error("proxy handler error", t)
  }

  private fun wrap(res: Any?) = Mapper.jackson.writeValueAsString(res)

  private fun Message<JsonObject>.response(res: Any?) = reply(wrap(res))

  override fun handle(msg: Message<JsonObject>) {
    try {
      val json = msg.body()
      val action = msg.headers().get("action") ?: throw IllegalStateException("action not specified")
      accessed()

      CoroutineScope(vertx.dispatcher()).launch {
        try {
          when (action) {
            
            "checkOrder" -> {
              msg.response(
                service.checkOrder(
                  json.getInteger("uid")
                )
              )
            }
            "create" -> {
              msg.response(
                service.create(
                  json.getString("id"),
                  json.getJsonArray("list").map{(it as JsonObject).map.entries.first().toPair() as Pair<String,Int>}
                )
              )
            }
            "getAllChecked" -> {
              msg.response(
                service.getAllChecked(
                  
                )
              )
            }
            "getAllNotChecked" -> {
              msg.response(
                service.getAllNotChecked(
                  json.getString("worker")
                )
              )
            }
            else -> throw IllegalStateException("Invalid action: $action")
          }
        } catch (t: Throwable) {
          failedHandler(msg, t)
        }
      }
    } catch (t: Throwable) {
      failedHandler(msg, t)
    }
  }
}