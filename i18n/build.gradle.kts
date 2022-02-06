plugins {
  kotlin("multiplatform")
  id("org.jetbrains.dokka")
  jacoco
  `maven-publish`
}

val jsMain by kotlin.sourceSets.getting {
  dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-html:${Versions.kotlinxHtml}")
  }
}
