buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    val kotlinVersion: String by project.extra
    classpath(kotlin("gradle-plugin", kotlinVersion))
  }
}
plugins {
  java
}

apply(plugin = "kotlin")

repositories {
  jcenter()
}

dependencies {
  val kotlinVersion: String by project.extra

  implementation(gradleApi())
  implementation("org.eclipse.jgit:org.eclipse.jgit:5.1.3.201810200350-r")
  implementation(kotlin("stdlib-jdk8"))
}
