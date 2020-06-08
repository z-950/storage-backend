import kotlin.collections.listOf

val projectList = listOf(
  "product",
  "order"
)

dependencies {
  implementation(kotlin("reflect"))
  implementation(project(":common"))

  projectList.forEach {
    implementation(project(":$it"))
  }
}

application {
  mainClassName = "pers.z950.codegen.MainKt"
}

tasks.register("gen"){
  val baseDir = "${project.projectDir.absolutePath}/src/main/kotlin/pers/z950/codegen"
  doFirst {
    // clean
    projectList.forEach {
      val dir = baseDir.replace("codegen", it)
      val ebProxyPath = "$dir/${it.capitalize()}ServiceVertxEBProxy.kt"
      val proxyHandlerPath = "$dir/${it.capitalize()}ServiceVertxProxyHandler.kt"

      println(ebProxyPath)

      delete(file(ebProxyPath))
      delete(file(proxyHandlerPath))
    }
  }

  finalizedBy("run")
}
