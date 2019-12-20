import org.gradle.api.publish.maven.internal.artifact.FileBasedMavenArtifact
import org.openstreetmap.josm.gradle.plugin.Versions

import proguard.gradle.ProGuardTask

buildscript {
  dependencies {
    classpath("net.sf.proguard:proguard-gradle:6.2.2")
  }
}
plugins {
  java
  application
}
apply(plugin = "kotlin")

dependencies {
  implementation(kotlin("stdlib-jdk8", Versions.kotlin))
  implementation(project(":i18n"))
}

application.mainClassName = "org.openstreetmap.josm.gradle.plugin.langconv.MainKt"

tasks.withType(JavaExec::class).getByName("run") {
  doFirst {
    if (args?.isEmpty() ?: true) {
      project.logger.warn("Note: Arguments for langconv can be supplied via the Gradle command line argument `--args=\"…\"`\n")
    }
  }
}

val jarTask: Jar = tasks.getByName<Jar>(sourceSets.main.get().jarTaskName) {
  description = "Build a runnable \"fat\" *.jar file with all dependencies."

  val i18nProj = project(":i18n")
  dependsOn(i18nProj.tasks[i18nProj.sourceSets.main.get().jarTaskName])

  manifest {
    attributes["Main-Class"] = application.mainClassName
  }
  doFirst {
    from(
      sourceSets.main.get().runtimeClasspath
        .filter { it.exists() }
        .mapNotNull { if (it.isDirectory) null else zipTree(it) }
    )
  }
}

val standaloneJar = tasks.register<ProGuardTask>("standaloneJar") {
  dependsOn(jarTask)

  injars(jarTask.archiveFile.get().asFile)
  outjars(File(buildDir, "/dist/${project.name}.jar"))

  jarTask.finalizedBy(this)
  if (JavaVersion.current().isJava9Compatible) { // >= JDK9
    libraryjars("${System.getProperty("java.home")}/jmods")
  } else { // < JDK9
    libraryjars("${System.getProperty("java.home")}/lib/rt.jar")
  }


  dontobfuscate()
  keep("public class org.openstreetmap.josm.gradle.plugin.langconv.** { *; }")
  keep("public class org.openstreetmap.josm.gradle.plugin.i18n.** { *; }")
  keepnames("class kotlin.** { *; }")
}

rootProject.extensions.getByType(PublishingExtension::class).apply {
  publications {
    val langconvPub = create<MavenPublication>("langconv") {
      groupId = "org.openstreetmap.josm"
      artifactId = "langconv"
      version = project.version.toString()

      standaloneJar.get().outputs.files.map { this.artifact(FileBasedMavenArtifact(it)) }
    }

    rootProject.tasks.withType(PublishToMavenRepository::class) {
      if (publication == langconvPub) {
        this.dependsOn(standaloneJar)
      }
    }
  }
}
