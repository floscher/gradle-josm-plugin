import java.net.URL

plugins {
  id("org.openstreetmap.josm").version("0.6.5")
}

base.archivesBaseName = "MyAwesomePlugin"

sourceSets.main {
  resources.exclude { it.name == "README.md" }
}

/**
 * For documentation about the `josm{}` block see
 * https://floscher.gitlab.io/gradle-josm-plugin/kdoc/latest/gradle-josm-plugin/org.openstreetmap.josm.gradle.plugin.config/-josm-plugin-extension/
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
    pluginDependencies.addAll(setOf("apache-commons", "apache-http"))
    website = URL("https://example.org")
    oldVersionDownloadLink(123, "1.2.0", URL("https://example.org/download/v1.2.0/MyAwesomePlugin.jar"))
    oldVersionDownloadLink( 42, "1.0.0", URL("https://example.org/download/v1.0.0/MyAwesomePlugin.jar"))
  }
  // The following block is optional, only needed when doing i18n for the plugin
  i18n {
    pathTransformer = getPathTransformer("gitlab.com/floscher/gradle-josm-plugin/blob/master/demo")
    bugReportEmail = "me@example.com"
    copyrightHolder = "John Doe"
  }
}
