plugins {
  kotlin("jvm").version(Versions.kotlin)
  kotlin("plugin.serialization").version(Versions.kotlin)
  `java-library`
}

repositories {
  gradlePluginPortal()
  mavenCentral()
}
java.sourceCompatibility = JavaVersion.VERSION_1_8
tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
  kotlinOptions {
    jvmTarget = java.sourceCompatibility.toString()
  }
}

kotlin {
  explicitApi()
  sourceSets.main {
    kotlin.srcDir(projectDir.resolve("../dogfood/src/main/kotlin/"))
  }
}

dependencies {
  implementation(gradleApi())
  implementation(gradleKotlinDsl())
  implementation(kotlin("stdlib"))
  implementation("org.eclipse.jgit:org.eclipse.jgit:${Versions.jgit}") {
    because("Newer versions require Java 11")
  }
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinSerialization}")

  // Version management for plugins used in subprojects
  implementation("com.gradle.publish:plugin-publish-plugin:${Versions.pluginPublish}")
  implementation(kotlin("gradle-plugin", Versions.kotlin))
  implementation("com.guardsquare:proguard-gradle:${Versions.proguardGradle}") {
    because("Newer versions require Java 11")
  }
  implementation("org.jetbrains.dokka:dokka-gradle-plugin:${Versions.dokka}")
  implementation("org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}")
}
