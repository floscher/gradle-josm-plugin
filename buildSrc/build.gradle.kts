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
  }
}
plugins {
  `java-library`
}

apply(plugin = "kotlin")

repositories {
  jcenter()
}

val dualSourceSet = sourceSets.create("dual")

tasks.withType(Jar::class).getByName("jar") {
  from(dualSourceSet.output)
}

val versions: Map<String, String> by project.extra

dependencies {

  add(dualSourceSet.implementationConfigurationName, gradleApi())
  add(dualSourceSet.implementationConfigurationName, "org.eclipse.jgit:org.eclipse.jgit:${versions["jgit"]}")
  add(dualSourceSet.implementationConfigurationName, kotlin("stdlib-jdk8", versions["kotlin"]))

  implementation(kotlin("gradle-plugin", versions["kotlin"]))
}

afterEvaluate {
  // Add dependencies from `dual` source set to runtime classpath of main source set,
  // so these are available in Gradle build script of root project.
  this.sourceSets.main.get().runtimeClasspath += dualSourceSet.compileClasspath
}
