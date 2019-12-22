//include(":demo")
include(":i18n")
include(":langconv")


// Allow to test out the demo project with an unpublished version of the plugin in the Maven repo inside the build directory
pluginManagement.repositories {
  maven(File(rootProject.projectDir, "build/maven"))
  gradlePluginPortal()
}

if (System.getenv("USE_LOCAL_PLUGIN_VERSION") == "true") {
  // Override the plugin version with the version published in the Maven repo inside the build directory
  pluginManagement {
    resolutionStrategy {
      eachPlugin {
        if (requested.id.id == "org.openstreetmap.josm") {
          this.useVersion(File(rootProject.projectDir, "build/maven/org/openstreetmap/josm/gradle-josm-plugin").listFiles()!!.singleOrNull { it.isDirectory }!!.name)
        }
      }
    }
  }
}
