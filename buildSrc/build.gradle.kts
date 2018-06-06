import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion: String by extra
buildscript {
  val kotlinVersion: String by extra { "1.2.41" }
  repositories {
    jcenter()
  }
  dependencies {
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
  implementation("org.eclipse.jgit:org.eclipse.jgit:5.0.0.201805301535-rc2")
  implementation(kotlin("stdlib-jdk8", kotlinVersion))
}
