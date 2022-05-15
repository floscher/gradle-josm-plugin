import java.net.URL

plugins {
  java
  id("org.openstreetmap.josm") // append `.version("‹current version number›")` to this line when using in a real project
}

josm.pluginName = "MyAwesomePlugin"

/**
 * For documentation about the `josm{}` block see
 * https://josm.gitlab.io/gradle-josm-plugin/kdoc/latest/gradle-josm-plugin/org.openstreetmap.josm.gradle.plugin.config/-josm-plugin-extension/
 */
josm {
  josmCompileVersion = "tested" // you can use either numbers or one of the special values `tested` or `latest` here
  manifest {
    description = "The description of my awesome JOSM plugin"
    mainClass = "org.openstreetmap.josm.plugins.myawesomeplugin.MyAwesomePlugin"
    minJosmVersion = "14140"

    // The following properties are entirely optional
    author = "John Doe"
    canLoadAtRuntime = true
    iconPath = "path/to/my/icon.svg"
    loadEarly = false
    loadPriority = 50
    pluginDependencies.addAll("apache-commons", "apache-http")
    website = URL("https://example.org")
    oldVersionDownloadLink(123, "1.2.0", URL("https://example.org/download/v1.2.0/MyAwesomePlugin.jar"))
    oldVersionDownloadLink( 42, "1.0.0", URL("https://example.org/download/v1.0.0/MyAwesomePlugin.jar"))
    classpath("./additional/path/for/the/classpath")
  }
  // The following block is optional, only needed when doing i18n for the plugin
  i18n {
    pathTransformer = getPathTransformer(projectDir, "gitlab.com/JOSM/gradle-josm-plugin/-/blob/main/demo")
    bugReportEmail = "me@example.com"
    copyrightHolder = "John Doe"
  }
}
