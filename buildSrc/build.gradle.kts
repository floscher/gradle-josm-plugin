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
    maven {
      this.url = uri("https://kotlin.bintray.com/kotlinx")
    }
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
    this.url = uri("https://kotlin.bintray.com/kotlinx")
  }
}

val versions: Map<String, String> by project.extra

val dualSourceSet = sourceSets.create("dual")

val kotlinSerializationConfiguration = configurations.detachedConfiguration().also { configuration ->
  configuration.dependencies.add(
    (dependencies.create("org.jetbrains.kotlinx:kotlinx-serialization-runtime:${versions["kotlinSerialization"]}") as ModuleDependency).also {
      it.exclude("org.jetbrains.kotlin")
    }
  )
}

val dualJarTask = tasks.register<Jar>("dualJar") {
  from(dualSourceSet.output)
  from(kotlinSerializationConfiguration.map {zipTree(it)})
  archiveBaseName.set("dual")
  destinationDirectory.set(File("$buildDir/libs"))
}

tasks.withType(Jar::class).getByName("jar") {
  from(dualSourceSet.output)
}

dualSourceSet.compileClasspath += kotlinSerializationConfiguration

dependencies {
  dualSourceSet.implementationConfigurationName.let {
    add(it, gradleApi())
    add(it, "org.eclipse.jgit:org.eclipse.jgit:${versions["jgit"]}")
    add(it, kotlin("stdlib", versions["kotlin"]))
    (dependencies.create("org.jetbrains.kotlinx:kotlinx-serialization-runtime:${versions["kotlinSerialization"]}") as ModuleDependency).also {
      it.exclude("org.jetbrains.kotlinx")
    }
  }

  // Add kotlin and kotlinx-serialization plugins to classpath of root build script
  implementation(kotlin("gradle-plugin", versions["kotlin"]))
  implementation(kotlin("serialization", versions["kotlin"]))
}

afterEvaluate {
  // Add dependencies from `dual` source set to runtime classpath of main source set,
  // so these are available in Gradle build script of root project.
  this.sourceSets.main.get().runtimeClasspath += dualSourceSet.compileClasspath
}
