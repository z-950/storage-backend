import java.io.ByteArrayOutputStream

ext {
  set("jacksonVersion", "2.10.2")
}

dependencies {
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${project.ext["jacksonVersion"]}")

  runtimeOnly("io.vertx:vertx-hazelcast:${project.ext["vertxVersion"]}")
}

tasks{
  shadowJar {
    archiveClassifier.set("fat")
  }

  register("list") {
    group = "dev"
    dependsOn(shadowJar)

    doLast {
      val fatjar = "${project.name}-${project.version}-fat.jar"
      val jarPath = "${project.buildDir.absolutePath}/libs/$fatjar"

      // java -jar common.jar list
      exec {
        workingDir = project.projectDir.absoluteFile
        commandLine("java", "-jar", jarPath, "list")
      }
    }
  }

  register("stop") {
    group = "dev"
    dependsOn(shadowJar)

    doLast {
      val fatjar = "${project.name}-${project.version}-fat.jar"
      val jarPath = "${project.buildDir.absolutePath}/libs/$fatjar"

      val out = ByteArrayOutputStream()
      exec {
        standardOutput = out
        workingDir = project.projectDir.absoluteFile
        this.commandLine("java", "-jar", jarPath, "list")
      }

      val str = out.toString()
      val arr = str.split(Regex("\n?\r|\r?\n")).filter { it.isNotBlank() }
      val appList = arr.subList(arr.indexOfFirst { it.contains("Listing vert.x applications") } + 1, arr.size)
      if (!appList.any { it.contains("No vert.x application found") }) {
        appList.forEach { line ->
          val id = line.split("\t").first()
          exec {
            workingDir = project.projectDir.absoluteFile
            commandLine("java", "-jar", jarPath, "stop", id)
          }
        }
      } else {
        println("No vert.x application found")
      }
    }
  }

  register("release") {
    group = "distribution"
    dependsOn("shadowJar")
  }
}
