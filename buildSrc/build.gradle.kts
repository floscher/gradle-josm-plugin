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
}

val versions: Map<String, String> by project.extra

val dualSourceSet = sourceSets.create("dual")

dependencies {
  add(dualSourceSet.implementationConfigurationName, gradleApi())
  add(dualSourceSet.implementationConfigurationName, "org.eclipse.jgit:org.eclipse.jgit:${versions["jgit"]}")
  add(dualSourceSet.implementationConfigurationName, kotlin("stdlib", versions["kotlin"]))
  add(dualSourceSet.implementationConfigurationName, (dependencies.create("org.jetbrains.kotlinx:kotlinx-serialization-runtime:${versions["kotlinSerialization"]}") as ModuleDependency).also {
    it.exclude("org.jetbrains.kotlinx")
  })

  // Add kotlin and kotlinx-serialization plugins to classpath of root build script
  implementation(kotlin("gradle-plugin", versions["kotlin"]))
  implementation(kotlin("serialization", versions["kotlin"]))

  // Add dual source set to classpath of buildscript of root project
  implementation(dualSourceSet.output)
  runtimeOnly(dualSourceSet.runtimeClasspath)
}
