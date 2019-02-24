import java.net.URI

buildscript {
  // Workaround to get the version numbers
  val versions by project.extra {
    File(projectDir, "src/main/kotlin/Versions.kt")
      .useLines {
        it.mapNotNull {
          Regex("""\s*const val ([a-zA-Z0-9]+) = "([^"]+)"""").matchEntire(it)?.let { it.groupValues[1] to it.groupValues[2] }
        }.toMap()
      }
  }

  repositories {
    jcenter()
  }
  dependencies {
    classpath(kotlin("gradle-plugin", versions["kotlin"]))
    classpath(kotlin("serialization", versions["kotlin"]))
  }
}
plugins {
  `java-library`
}

apply(plugin = "kotlin")
apply(plugin = "kotlinx-serialization")

repositories {
  jcenter()
  maven {
    this.url = URI("https://kotlin.bintray.com/kotlinx")
  }
}

val dualSourceSet = sourceSets.create("dual")

tasks.withType(Jar::class).getByName("jar") {
  from(dualSourceSet.output)
}

val versions: Map<String, String> by project.extra

dependencies {

  listOf(dualSourceSet, sourceSets.main.get()).map { it.implementationConfigurationName }.forEach {
    this.add(it, gradleApi())
    this.add(it, "org.eclipse.jgit:org.eclipse.jgit:${versions["jgit"]}")
  }

  add(dualSourceSet.implementationConfigurationName, kotlin("stdlib-jdk8", versions["kotlin"]))

  implementation(kotlin("gradle-plugin", versions["kotlin"]))
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:${versions["kotlinSerialization"]}")
}

afterEvaluate {
  // Add dependencies from `dual` source set to runtime classpath of main source set,
  // so these are available in Gradle build script of root project.
  this.sourceSets.main.get().runtimeClasspath += dualSourceSet.compileClasspath
}
