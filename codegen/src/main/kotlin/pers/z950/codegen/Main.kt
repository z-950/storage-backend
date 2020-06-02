package pers.z950.codegen

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import pers.z950.product.ProductService
import java.io.File
import kotlin.reflect.*
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName

val list: List<KClass<*>> = listOf(
  ProductService::class
)
val baseDir = System.getProperty("user.dir") + "/src/main/kotlin"

fun main() {
  gen()
}

fun gen() {
  list.forEach {
    val packageName = it.java.`package`.name
    val packagePath = packageName.replace(".", "/")
    val path = "${baseDir.replace("codegen", packageName.split(".").last())}/$packagePath/"

    val eBProxyFileUrl = "$path${it.simpleName}VertxEBProxy.kt"
    val eBProxyFile = File(eBProxyFileUrl)
    if (!eBProxyFile.exists()) eBProxyFile.createNewFile()
    eBProxyFile.writeText(getEBProxy(it))

    val proxyHandlerFileUrl = "$path${it.simpleName}VertxProxyHandler.kt"
    val proxyHandlerFile = File(proxyHandlerFileUrl)
    if (!proxyHandlerFile.exists()) proxyHandlerFile.createNewFile()
    proxyHandlerFile.writeText(getProxyHander(it))
  }
}

fun getTypeFullSimpleName(type: KType): String {
  val typeName = type.jvmErasure.simpleName!!

  val generics = type.arguments.joinToString(",") { getTypeFullSimpleName(it.type!!) }

  return if (generics.isEmpty()) {
    typeName
  } else {
    "$typeName<$generics>"
  }
}

fun getEBProxy(clazz: KClass<*>) = """
// code gen
${clazz.java.`package`}

import com.fasterxml.jackson.core.type.TypeReference
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.kotlin.core.eventbus.requestAwait
import io.vertx.kotlin.core.json.jsonObjectOf

class ${clazz.simpleName}VertxEBProxy constructor(private val vertx: Vertx, private val address: String, private val options: DeliveryOptions) : ${clazz.simpleName} {
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

  ${genMethod(clazz)}
}
""".trimIndent()

fun genMethod(clazz: KClass<*>): String = clazz.declaredFunctions.joinToString("") {
  """
  override suspend fun ${it.name}(${genArgs(it)}):${getTypeFullSimpleName(it.returnType)} {
    val jsonArgs = jsonObjectOf(${putJson(it)})
    return getEventBusReplyValue("${it.name}", jsonArgs)
  }
"""
}

private fun genArgs(function: KFunction<*>): String = function.valueParameters.joinToString(", ") {
  "${it.name}:${getTypeFullSimpleName(it.type)}"
}

private fun putJson(function: KFunction<*>): String = function.valueParameters.joinToString(", ") {
  "\"${it.name}\" to ${it.name}"
}

fun getProxyHander(clazz: KClass<*>) = """
// code gen
${clazz.java.`package`}

import pers.z950.common.service.ServiceException
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.core.logging.LoggerFactory
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.serviceproxy.ProxyHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ${clazz.simpleName}VertxProxyHandler(private val vertx: Vertx, private val service: ${clazz.simpleName}, topLevel: Boolean = true, private val timeoutSeconds: Long = 300) : ProxyHandler() {
  private val log = LoggerFactory.getLogger(${clazz.simpleName}VertxProxyHandler::javaClass.name)

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

  private fun wrap(res: Any?) = DatabindCodec.mapper().writeValueAsString(res)

  private fun Message<JsonObject>.response(res: Any?) = reply(wrap(res))

  override fun handle(msg: Message<JsonObject>) {
    try {
      val json = msg.body()
      val action = msg.headers().get("action") ?: throw IllegalStateException("action not specified")
      accessed()

      CoroutineScope(vertx.dispatcher()).launch {
        try {
          when (action) {
            ${genWhenStatement(clazz)}
            else -> throw IllegalStateException("Invalid action: ${"$" + "action"}")
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
""".trimIndent()

fun genWhenStatement(clazz: KClass<*>): String = clazz.declaredFunctions.joinToString("") {
  """
            "${it.name}" -> {
              msg.response(
                service.${it.name}(
                  ${genJsonPara(it)}
                )
              )
            }"""
}

fun genJsonPara(function: KFunction<*>): String = function.valueParameters.joinToString(",\n                  ") {
  getJsonGetStr(it.type, it.name!!)
}

// todo: more type
fun getJsonGetStr(type: KType, key: String): String = when (type.jvmErasure) {
  Boolean::class -> "json.getBoolean(\"$key\")"
  Byte::class -> "json.getInteger(\"$key\").toByte()"
  Short::class -> "json.getInteger(\"$key\").toShort()"
  Int::class -> "json.getInteger(\"$key\")"
  Long::class -> "json.getLong(\"$key\")"
  Float::class -> "json.getFloat(\"$key\")"
  Double::class -> "json.getDouble(\"$key\")"
  String::class -> "json.getString(\"$key\")"
  List::class -> "json.getJsonArray(\"$key\").list as ${getTypeFullSimpleName(type)}"
  Map::class -> "json.getJsonObject(\"$key\").map as ${getTypeFullSimpleName(type)}"
  JsonObject::class -> "json.getJsonObject(\"$key\")"
  JsonArray::class -> "json.getJsonArray(\"$key\")"
  else -> " type" + type.jvmErasure.jvmName + " is not found!!!"
}
