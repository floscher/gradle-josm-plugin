import java.net.URI

pluginManagement {
  repositories {
    jcenter()
    gradlePluginPortal()
    maven {
      url = URI("https://dl.bintray.com/kotlin/kotlin-eap")
    }
  }
  resolutionStrategy {
    eachPlugin {
      if (requested.id.namespace == "org.jetbrains.kotlin") {
        val kotlinVersion: String? by gradle.rootProject.extra { requested.version }
      }
      if (requested.id.namespace == "org.jetbrains" && requested.id.name == "dokka") {
        useModule("org.jetbrains.dokka:dokka-gradle-plugin:${requested.version}")
      }
      if (requested.id.namespace == "org.junit.platform.gradle") {
        useModule("org.junit.platform:junit-platform-gradle-plugin:${requested.version}")
      }
    }
  }
}
