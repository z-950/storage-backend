package pers.z950.common

import io.vertx.core.DeploymentOptions
import io.vertx.core.Verticle
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.deployVerticleAwait
import io.vertx.kotlin.core.undeployAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.serviceproxy.ServiceBinder

abstract class DeployableVerticle : CoroutineVerticle() {
  companion object {
    const val SERVICE_CONFIG_KEY = "service"
    const val API_CONFIG_KEY = "api"
  }

  private lateinit var binder: ServiceBinder
  private lateinit var consumer: MessageConsumer<JsonObject>
  private val verticleList: MutableList<String> = mutableListOf()

  protected suspend fun deployVerticle(verticle: Verticle, options: DeploymentOptions) {
    verticleList.add(vertx.deployVerticleAwait(verticle, options))
  }

  protected fun <T : Any> proxyHelper(clazz: Class<T>, impl: T) {
    binder = ServiceBinder(vertx)
    consumer = binder.setAddress("service.${clazz.simpleName.toLowerCase().replace("service", "")}").register(clazz, impl)
  }

  override suspend fun stop() {
    if (this::binder.isInitialized && this::consumer.isInitialized) {
      binder.unregister(consumer)
    }

    verticleList.forEach {
      vertx.undeployAwait(it)
    }

    super.stop()
  }
}
