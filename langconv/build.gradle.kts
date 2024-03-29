import org.gradle.jvm.tasks.Jar
import proguard.gradle.ProGuardTask

plugins {
  application
  kotlin("jvm")
  `maven-publish`
  id("org.jetbrains.dokka")
}

dependencies {
  implementation(project(":common"))
  implementation(project(":i18n"))
}

application.mainClass.set("org.openstreetmap.josm.gradle.plugin.langconv.MainKt")
tasks.jar {
  manifest.attributes(mapOf("Main-Class" to application.mainClass))
}

val run by tasks.getting(JavaExec::class) {
  doFirst {
    if ((args ?: listOf()).isEmpty()) {
      project.logger.warn("Note: Arguments for langconv can be supplied via the Gradle command line argument `--args=\"…\"`\n")
    }
  }
}

val standaloneJar by tasks.registering(ProGuardTask::class) {
  description = "Builds a standalone runnable *.jar file"
  group = "Build"

  injars(mapOf("filter" to "!META-INF/**"), sourceSets.main.map { it.compileClasspath })
  injars(tasks.named(sourceSets.main.get().jarTaskName))
  outjars(File(buildDir, "/dist/${project.name}.jar"))

  if (JavaVersion.current().isJava9Compatible) { // >= JDK9
    libraryjars("${System.getProperty("java.home")}/jmods")
  } else { // < JDK9
    libraryjars("${System.getProperty("java.home")}/lib/rt.jar")
  }

  dontobfuscate()
  keep("public class org.openstreetmap.josm.gradle.plugin.langconv.** { *; }")
  keep("public class org.openstreetmap.josm.gradle.plugin.i18n.** { *; }")
}

tasks.sourcesJar {
  from(project(":i18n").tasks.named<Jar>("jvmSourcesJar").map { it.outputs.files.map { zipTree(it) } })
}

publishing {
  publications {
    register<MavenPublication>(project.name) {
      artifact(standaloneJar.map { it.outJarFiles.filterNotNull().single() })
      artifact(tasks.sourcesJar.flatMap { it.archiveFile }) {
        classifier = "sources"
      }
    }
  }
}
