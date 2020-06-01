package pers.z950.common

import io.vertx.core.impl.ConcurrentHashSet
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.servicediscovery.publishAwait
import io.vertx.kotlin.servicediscovery.unpublishAwait
import io.vertx.servicediscovery.Record
import io.vertx.servicediscovery.ServiceDiscovery
import io.vertx.servicediscovery.ServiceDiscoveryOptions
import io.vertx.servicediscovery.types.EventBusService
import io.vertx.servicediscovery.types.HttpEndpoint
import io.vertx.servicediscovery.types.MessageSource

abstract class MicroServiceVerticle : CoroutineVerticle() {
  companion object {
    private const val LOG_EVENT_ADDRESS = "event.log"
  }

  protected val log: Logger = LoggerFactory.getLogger(this.javaClass.name)

  protected lateinit var discovery: ServiceDiscovery
  private val registeredRecords = ConcurrentHashSet<Record>()

  override suspend fun start() {
    // init service discovery instance
    discovery = ServiceDiscovery.create(vertx, ServiceDiscoveryOptions().setBackendConfiguration(config))
  }

  override suspend fun stop() {
    registeredRecords.forEach { record ->
      discovery.unpublishAwait(record.registration)
    }

    discovery.close()

    super.stop()
  }


  suspend fun publishHttpEndpoint(name: String, host: String, port: Int) {
    val record = HttpEndpoint.createRecord(name, host, port, "/")
    publish(record)
  }

  suspend fun publishMessageSource(name: String, address: String, contentClass: Class<*>) {
    val record = MessageSource.createRecord(name, address, contentClass)
    publish(record)
  }

  suspend fun publishMessageSource(name: String, address: String) {
    val record = MessageSource.createRecord(name, address)
    publish(record)
  }

  suspend fun publishEventBusService(name: String, address: String, serviceClass: Class<*>) {
    val record = EventBusService.createRecord(name, address, serviceClass)
    publish(record)
  }

  private suspend fun publish(record: Record) {
    if (!this::discovery.isInitialized) {
      try {
        start()
      } catch (e: Exception) {
        throw RuntimeException("cannot create discovery service")
      }
    }
    discovery.publishAwait(record)
  }

  /**
   * A helper method that simply publish logs on the event bus.
   *
   * @param type log type
   * @param data log message data
   */
  open fun publishLogEvent(type: String?, data: JsonObject?) {
    vertx.eventBus().publish(LOG_EVENT_ADDRESS, jsonObjectOf("type" to type, "message" to data))
  }

  open fun publishLogEvent(type: String?, data: JsonObject?, succeeded: Boolean) {
    vertx.eventBus().publish(LOG_EVENT_ADDRESS, jsonObjectOf("type" to type, "status" to succeeded, "message" to data))
  }
}
