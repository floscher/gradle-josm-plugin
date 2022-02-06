import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPom

public fun Project.gradleJosmPluginMetadata(): (MavenPom) -> Unit = { pom ->
  pom.name.set("gradle-josm-plugin$path")
  pom.description.set(
    when (path) {
      ":common" ->
        "A small multiplatform (JavaScript & Java) library that provides a few useful tools for the " +
        "`gradle-josm-plugin` and the `langconv` CLI."
      ":dogfood" ->
        "A library that contains functionality, which is on the one hand used by the gradle-josm-plugin, " +
        "but on the other hand also used in the process of building the gradle-josm-plugin itself."
      ":i18n" ->
        "A multiplatform (JavaScript & Java) library for reading and writing GNU gettext files (*.mo and *.po files) " +
        "as well as a custom i18n file format used by JOSM (*.lang files)."
      ":langconv" ->
        "A self-contained Java command line utility for reading and writing GNU gettext files (*.mo and *.po files) " +
        "as well as a custom i18n file format used by JOSM (*.lang files)."
      ":plugin" -> "A Gradle plugin that helps with building plugins for the OpenStreetMap editor JOSM."
      else -> TODO("Add module description to pom.xml of project `${path}`")
    }
  )
  pom.url.set("https://josm.gitlab.io/gradle-josm-plugin")
  pom.licenses { lics ->
    lics.license { lic ->
      lic.name.set("GPL-3.0-or-later")
      lic.url.set("https://www.gnu.org/licenses/gpl-3.0")
      lic.distribution.set("repo")
    }
  }
  pom.organization { org ->
    org.name.set("JOSM")
    org.url.set("https://josm.openstreetmap.de")
  }
  pom.developers { devs ->
    devs.developer { dev ->
      dev.id.set("floscher")
      dev.name.set("Florian Schäfer")
      dev.url.set("https://gitlab.com/floscher")
      dev.timezone.set("Europe/Berlin")
    }
  }
  pom.contributors { contribs ->
    contribs.contributor { contrib ->
      contrib.name.set("Karl Guggisberg")
      contrib.url.set("https://github.com/Gubaer")
    }
    contribs.contributor { contrib ->
      contrib.name.set("Taylor Smock")
      contrib.url.set("https://gitlab.com/smocktaylor")
    }
  }
  pom.inceptionYear.set(
    when (path) {
      ":plugin" -> "2017"
      ":dogfood", ":i18n", ":langconv" -> "2020"
      ":common" -> "2022"
      else -> TODO("Add inception year for module $path")
    }
  )
  pom.scm { scm ->
    scm.developerConnection.set("scm:git:ssh://git@gitlab.com/JOSM/gradle-josm-plugin.git")
    scm.connection.set("scm:git:https://gitlab.com/JOSM/gradle-josm-plugin.git")
    scm.url.set("https://gitlab.com/JOSM/gradle-josm-plugin")
  }
  pom.issueManagement { issues ->
    issues.system.set("GitLab")
    issues.url.set("https://gitlab.com/JOSM/gradle-josm-plugin/-/issues")
  }
}
