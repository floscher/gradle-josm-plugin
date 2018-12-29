import org.gradle.api.publish.maven.internal.artifact.FileBasedMavenArtifact
import org.openstreetmap.josm.gradle.plugin.Versions

import proguard.gradle.ProGuardTask

buildscript {
  dependencies {
    classpath("net.sf.proguard:proguard-gradle:6.1.0beta1")
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
  val args: String? by project.extra
  args?.let{ setArgsString(it) }
  doFirst {
    if (args == null) {
      logger.warn("Arguments for langconv can be supplied via the Gradle command line argument `-Pargs=\"…\"`\n")
    }
  }
}

val jarTask: Jar = tasks.withType(Jar::class).getByName(sourceSets.main.get().jarTaskName) {
  description = "Build a runnable \"fat\" *.jar file with all dependencies."
  manifest {
    attributes["Main-Class"] = application.mainClassName
  }
}

val dependencyJar: Jar = tasks.create("dependencyJar", Jar::class) {
  val i18nProj = project(":i18n")
  dependsOn(jarTask, i18nProj.tasks[i18nProj.sourceSets.main.get().jarTaskName])
  jarTask.finalizedBy(this)
  doFirst {
    from(
      sourceSets.main.get().compileClasspath
        .filter { it.exists() }
        .map { if (it.isDirectory) fileTree(it) else zipTree(it) }
    )
  }

  appendix = "dependencies"
}

val standaloneJar = tasks.register<ProGuardTask>("standaloneJar") {
  dependsOn(dependencyJar)

  injars(jarTask.archivePath)
  injars(dependencyJar.archivePath)
  outjars(File(buildDir, "/dist/${project.name}.jar"))

  if (JavaVersion.current().isJava11Compatible) { // >= JDK11
    doFirst {
      TODO("Proguard currently does not support JDKs 11 or newer")
    }
  } else {
    dependencyJar.finalizedBy(this)
    if (JavaVersion.current().isJava9Compatible) { // >= JDK9
      libraryjars(listOf("${System.getProperty("java.home")}/jmods", "${System.getProperty("java.home")}/lib"))
    } else { // < JDK9
      libraryjars("${System.getProperty("java.home")}/lib/rt.jar")
    }
  }

  dontobfuscate()
  keep(
    """
      public class ${application.mainClassName} {
        public static void main(java.lang.String[]);
      }
    """.trimIndent()
  )
}

val distributionTask = if (JavaVersion.current().isJava11Compatible) tasks.named("distZip") else standaloneJar

rootProject.extensions.getByType(PublishingExtension::class).apply {
  publications {
    val langconvPub = create<MavenPublication>("langconv") {
      groupId = "org.openstreetmap.josm"
      artifactId = "langconv"
      version = project.version.toString()

      distributionTask.get().outputs.files.map { this.artifact(FileBasedMavenArtifact(it)) }
    }

    rootProject.tasks.withType(PublishToMavenRepository::class) {
      if (publication == langconvPub) {
        this.dependsOn(distributionTask)
      }
    }
  }
}
