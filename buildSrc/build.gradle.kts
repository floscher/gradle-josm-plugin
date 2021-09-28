plugins {
  kotlin("jvm").version(Versions.kotlin)
  kotlin("plugin.serialization").version(Versions.kotlin)
  `java-library`
}

repositories {
  gradlePluginPortal()
  jcenter()
  mavenCentral()
}

kotlin.sourceSets.main {
  kotlin.srcDir(projectDir.resolve("../dogfood/src/main/kotlin/"))
}

dependencies {
  implementation(gradleApi())
  implementation(gradleKotlinDsl())
  implementation("org.eclipse.jgit:org.eclipse.jgit:${Versions.jgit}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinSerialization}")

  // Version management for plugins used in subprojects
  implementation("com.gradle.publish:plugin-publish-plugin:${Versions.pluginPublish}")
  implementation(kotlin("gradle-plugin", Versions.kotlin))
  implementation("com.guardsquare:proguard-gradle:${Versions.proguardGradle}")
  implementation("org.jetbrains.dokka:dokka-gradle-plugin:${Versions.dokka}")
  implementation("org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}")
  implementation("com.jfrog.bintray.gradle:gradle-bintray-plugin:${Versions.bintray}")
}
