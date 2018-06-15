import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    val kotlinVersion: String by project.extra { "1.2.50" }
    classpath(kotlin("gradle-plugin", kotlinVersion))
  }
}
plugins {
  java
}
apply {
  plugin("org.jetbrains.kotlin.jvm")
}
repositories {
  jcenter()
}
dependencies {
  val kotlinVersion: String by rootProject.extra
  implementation("org.eclipse.jgit:org.eclipse.jgit:5.0.0.201805301535-rc2")
  implementation(kotlin("stdlib-jdk8", kotlinVersion))
}
