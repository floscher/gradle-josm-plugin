import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.openstreetmap.josm.gradle.plugin.task.gitlab.ReleaseToGitlab
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
  kotlin("jvm")
  id("com.gradle.plugin-publish")
  id("org.jetbrains.dokka")

  `java-gradle-plugin`
  `maven-publish`
}

dependencies {
  api(project(":dogfood"))
  api(project(":i18n"))
  implementation("com.squareup.okhttp3", "okhttp", Versions.okhttp)
  implementation("com.beust","klaxon", Versions.klaxon)
  implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", Versions.jackson)
  implementation("com.vladsch.flexmark:flexmark:${Versions.flexmark}")

  testImplementation("org.eclipse.jgit", "org.eclipse.jgit", Versions.jgit)
  testImplementation("org.junit.jupiter", "junit-jupiter-api", Versions.junit)
  testImplementation("org.junit.jupiter", "junit-jupiter-params", Versions.junit)
  testImplementation("com.github.tomakehurst","wiremock",Versions.wiremock)
  testImplementation("ru.lanwen.wiremock", "wiremock-junit5", Versions.wiremockJunit5)
  testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", Versions.junit)
}

tasks.sourcesJar {
  from(project.provider { project(":dogfood").sourceSets.main.map { it.allSource } })
  from(project.provider { project(":i18n").sourceSets.main.map { it.allSource } })
  exclude("packages.md")
}

tasks.javadocJar {
  from(tasks.named<DokkaTask>("dokkaHtml").map { it.outputDirectory })
  //from(provider { rootProject.tasks.getByName<DokkaMultiModuleTask>("dokkaHtmlMultiModule").outputDirectory })
}

// for the plugin-publish (publish to plugins.gradle.org)
pluginBundle {
  website = "https://gitlab.com/floscher/gradle-josm-plugin#readme"
  vcsUrl = "https://gitlab.com/floscher/gradle-josm-plugin.git"
  description = "This plugin helps with developing for the JOSM project."
  tags = listOf("josm", "openstreetmap", "osm")

  plugins.create("josmPlugin") {
    id = project.group.toString()
    displayName = "Gradle JOSM plugin"
  }
}
// for the java-gradle-plugin (local publishing)
gradlePlugin {
  plugins.create("josmPlugin") {
    id = project.group.toString()
    implementationClass = "org.openstreetmap.josm.gradle.plugin.JosmPlugin"
  }
}


publishing {
  publications.withType(MavenPublication::class).all {
    if (name == "pluginMaven") {
      artifactId = "gradle-josm-plugin"
    }
  }
}

// Create `releaseToGitlab` task that can publish a release based on a Gitlab Maven package for a tag.
val releaseToGitlab = tasks.create(
  "releaseToGitlab",
  ReleaseToGitlab::class,
  { true },
  setOf("org/openstreetmap/josm/gradle-josm-plugin", "org/openstreetmap/josm/langconv")
)
