import org.jetbrains.dokka.gradle.DokkaTask

plugins {
  `java-library`
  jacoco
}
apply(plugin = "kotlin")

dependencies {
  implementation(kotlin("stdlib-jdk8", Versions.kotlin))

  testImplementation("org.junit.jupiter", "junit-jupiter-api", Versions.junit)
  testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", Versions.junit)
}

attachToRootProject(rootProject, project)

fun attachToRootProject(rootProj: Project, i18nProj: Project) {
  // Tasks of root project that depend on their counterparts in this subproject
  listOf("check", "compileKotlin").forEach {
    rootProj.tasks.getByName(it).dependsOn(i18nProj.tasks.getByName(it))
  }

  // Add to sources JAR of root project
  rootProj.tasks.getByName("publishPluginJar", Jar::class) {
    from(i18nProj.sourceSets.main.get().allSource)
  }

  // Include this subproject in JaCoCo report of root project
  rootProj.tasks.getByName("jacocoTestReport", JacocoReport::class) {
    sourceSets(i18nProj.sourceSets.main.get())
    executionData(i18nProj.tasks.getByName("jacocoTestReport", JacocoReport::class).executionData)
  }

  // Include this subproject in Dokka docs of root project
  rootProj.tasks.withType(DokkaTask::class).getByName("dokka").configuration {
    i18nProj.sourceSets.main.get().allSource.srcDirs
      .filter { it.exists() }
      .forEach {
        sourceRoot {
          path = it.path
        }
      }
  }
}
