import java.net.URI

pluginManagement {
  repositories {
    jcenter()
    gradlePluginPortal()
  }
  resolutionStrategy {
    eachPlugin {
      if (requested.id.namespace == "org.jetbrains.kotlin") {
        val kotlinVersion: String? by gradle.rootProject.extra { requested.version }
      }
      if (requested.id.namespace == "org.junit.platform.gradle") {
        useModule("org.junit.platform:junit-platform-gradle-plugin:${requested.version}")
      }
    }
  }
}
