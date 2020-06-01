import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

buildscript {
  repositories {
    mavenCentral()
  }

  dependencies {
    classpath(kotlin("gradle-plugin", version = "1.3.71"))
  }
}

plugins {
  id("application")
  id("com.github.johnrengelman.shadow") version "5.2.0"
}

allprojects {
  group = "pers.z950"
  version = "1.0.0-SNAPSHOT"
}

fun subProjectsExcept(projects: List<String>) = subprojects.filter { !projects.contains(it.name) }
fun subProjectsExcept(projects: List<String>, block: (Project) -> Unit) {
  subprojects.filter { !projects.contains(it.name) }.forEach {
    block(it)
  }
}

subprojects {
  apply(plugin = "application")
  apply(plugin = "kotlin")
  apply(plugin = "com.github.johnrengelman.shadow")

  repositories {
    mavenCentral()
    jcenter()
  }

  ext {
    set("kotlinVersion", "1.3.71")
    set("kotlinxCoroutinesVersion", "1.3.5")
    set("vertxVersion", "3.9.0")
  }

  dependencies {
    implementation("io.vertx:vertx-core:${project.ext["vertxVersion"]}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${project.ext["kotlinVersion"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${project.ext["kotlinxCoroutinesVersion"]}")
  }

  val compileKotlin: KotlinCompile by tasks
  compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
  }

  val compileTestKotlin: KotlinCompile by tasks
  compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
  }
}

subProjectsExcept(listOf("codegen")) {
  it.run {
    ext {
      set("junitJupiterEngineVersion", "5.4.0")
      set("logVersion", "2.13.2")
    }

    dependencies {
      implementation("io.vertx:vertx-lang-kotlin-coroutines:${project.ext["vertxVersion"]}")
      implementation("io.vertx:vertx-lang-kotlin:${project.ext["vertxVersion"]}")
      implementation("io.vertx:vertx-web:${project.ext["vertxVersion"]}")
      implementation("io.vertx:vertx-web-client:${project.ext["vertxVersion"]}")
      implementation("io.vertx:vertx-service-discovery:${project.ext["vertxVersion"]}")
      implementation("io.vertx:vertx-service-proxy:${project.ext["vertxVersion"]}")
      implementation("io.vertx:vertx-circuit-breaker:${project.ext["vertxVersion"]}")
      implementation("io.vertx:vertx-pg-client:${project.ext["vertxVersion"]}")
      implementation("io.vertx:vertx-auth-common:${project.ext["vertxVersion"]}")
      implementation("io.vertx:vertx-auth-shiro:${project.ext["vertxVersion"]}")

      implementation("org.apache.logging.log4j:log4j-slf4j18-impl:${project.ext["logVersion"]}")
      runtimeOnly("org.apache.logging.log4j:log4j-core:${project.ext["logVersion"]}")

      testImplementation("io.vertx:vertx-junit5:${project.ext["vertxVersion"]}")
      testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${project.ext["junitJupiterEngineVersion"]}")
      testImplementation("org.junit.jupiter:junit-jupiter-api:${project.ext["junitJupiterEngineVersion"]}")
    }

    application {
      mainClassName = "pers.z950.cli.Launcher"
    }

    tasks.withType<Test> {
      useJUnitPlatform()
      testLogging {
        events = mutableSetOf(PASSED, FAILED, SKIPPED)
      }
    }

    tasks.clean {
      doFirst {
        val logDir = "${project.projectDir.absolutePath}/logs"
        delete(files(logDir))
        println(targetFiles.asPath)
      }
    }
  }
}

//  dependencies {
//    implementation("io.vertx:vertx-health-check:${project.ext["vertxVersion"]}")
//    implementation("io.vertx:vertx-mail-client:${project.ext["vertxVersion"]}")
//  }

rootProject.tasks {
  register("startAll") {
    group = "dev"

    setDependsOn(
      listOf(
        project("cli").tasks.getByName("stop"),
        project("gateway").tasks.getByName("run", JavaExec::class)
      ).plus(subProjectsExcept(listOf("codegen", "cli", "common"))
        .map { it.tasks.getByName("run", JavaExec::class) })
    )
  }
  register("list") {
    group = "dev"

    setDependsOn(listOf(project("cli").tasks.getByName("list")))
  }
  register("stop") {
    group = "dev"

    setDependsOn(listOf(project("cli").tasks.getByName("stop")))
  }
  register("release") {
    group = "distribution"

    setDependsOn(subProjectsExcept(listOf("codegen", "common")).map { it.tasks.getByName("release") })
  }
}

subProjectsExcept(listOf("codegen", "cli", "common")) { project ->
  project.tasks.getByName("run", JavaExec::class) {
    doFirst {
      println(commandLine.joinToString(" "))
    }

    val mainVerticle = "pers.z950.${project.name}.${project.name.capitalize()}Verticle"

    main = project.application.mainClassName

    workingDir(File(project.projectDir.absolutePath))
    val cmd = System.getProperty("cmd", "run").toLowerCase()
    args = listOf(cmd, mainVerticle)
  }

  project.tasks.shadowJar {
    val mainVerticle = "pers.z950.${project.name}.${project.name.capitalize()}Verticle"

    archiveClassifier.set("fat")
    manifest.attributes(
      "Main-Verticle" to mainVerticle
    )
  }

  project.tasks.register("release") {
    group = "distribution"
    dependsOn("shadowJar")

    // todo: copy
  }

  project.tasks.register("runJar") {
    group = "dev"
    dependsOn("shadowJar")

    doLast {
      val fatjar = "${project.name}-${project.version}-fat.jar"
      val jarPath = "${project.buildDir.absolutePath}/libs/$fatjar"

      exec {
        workingDir = project.projectDir.absoluteFile
        // use start cmd to run background
        commandLine("java", "-jar", jarPath, "start", "--redirect-output")
//        commandLine("java", "-jar", jarPath, "start")
      }
    }
  }
}
