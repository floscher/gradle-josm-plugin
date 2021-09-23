import Build_gradle.IKotlinCompile
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openstreetmap.josm.gradle.plugin.api.gitlab.setupGitlabPublishingForAllProjects
import org.openstreetmap.josm.gradle.plugin.logCoverage
import org.openstreetmap.josm.gradle.plugin.logPublishedMavenArtifacts
import org.openstreetmap.josm.gradle.plugin.logSkippedTasks
import org.openstreetmap.josm.gradle.plugin.logTaskDuration
import org.openstreetmap.josm.gradle.plugin.GitDescriber
import java.net.URL

typealias IKotlinCompile<T> = org.jetbrains.kotlin.gradle.dsl.KotlinCompile<T>

plugins {
  id("org.jetbrains.dokka")
  jacoco
}

gradle.projectsEvaluated {
  allprojects {
    extensions.findByType(JacocoPluginExtension::class)?.toolVersion = Versions.jacoco
  }
  val jacocoTestReport by tasks.registering(JacocoReport::class) {
    group = "Verification"
    val testTasks = setOf(
      project(":i18n").tasks.getByName<Test>("jvmTest"),
      project(":plugin").tasks.getByName<Test>("test")
    )

    executionData(* testTasks.toTypedArray())
    dependsOn(testTasks)

    additionalClassDirs(project(":i18n").tasks.getByName<KotlinCompile>("compileKotlinJvm").destinationDir)
    additionalSourceDirs(project(":i18n").extensions.getByType(KotlinProjectExtension::class).sourceSets.getByName("commonMain").kotlin.sourceDirectories)
    additionalSourceDirs(project(":i18n").extensions.getByType(KotlinProjectExtension::class).sourceSets.getByName("jvmMain").kotlin.sourceDirectories)

    sourceSets(*
      setOf(":dogfood", ":langconv", ":plugin")
        .map { project(it).extensions.getByType(SourceSetContainer::class).getByName(SourceSet.MAIN_SOURCE_SET_NAME) }
        .toTypedArray()
    )
  }
}

val javaVersion = JavaVersion.VERSION_1_8

// Logging
gradle.taskGraph.logPublishedMavenArtifacts()
gradle.taskGraph.logTaskDuration()
logSkippedTasks()

allprojects {
  group = "org.openstreetmap.josm" + if (name != "plugin") ".gradle-josm-plugin" else ""
  version = GitDescriber(rootProject.projectDir).describe()

  repositories.jcenter()
  repositories.mavenCentral()

  tasks.withType(JacocoReport::class).all {
    tasks.findByName("test")?.let { dependsOn(it) }
    logCoverage()
  }

  tasks.withType(Test::class).all {
    useJUnitPlatform()
    testLogging {
      lifecycle {
        events = setOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.STANDARD_ERROR)
      }
    }
  }
  tasks.withType(IKotlinCompile::class).all {
    if (this is KotlinCompile) {
      kotlinOptions.jvmTarget = javaVersion.toString()
    }
    kotlinOptions {
      freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
  }
  tasks.withType(DokkaTask::class) {
    dokkaSourceSets.all {
      skipEmptyPackages.set(false)

      if (platform.get() == Platform.jvm) {
        jdkVersion.set(javaVersion.ordinal + 1)
        externalDocumentationLink(URL("https://docs.gradle.org/${gradle.gradleVersion}/javadoc/"))
        externalDocumentationLink(URL("http://docs.groovy-lang.org/next/html/api/"))
      }

      project.projectDir.resolve("packages.md")
        .takeIf { it.isFile }
        ?.let { packagesFile ->
          includes.from(packagesFile)
        }
    }
  }

  pluginManager.withPlugin("java") {
    extensions.findByType(JavaPluginExtension::class)?.apply {
      sourceCompatibility = javaVersion
      withJavadocJar()
      withSourcesJar()
    }
  }

  pluginManager.withPlugin("publishing") {
    afterEvaluate {
      tasks.withType(Jar::class).findByName("javadocJar")?.apply {
        from(tasks.named<DokkaTask>("dokkaHtml").map { it.outputDirectory })
      }
    }
  }
}

tasks.dokkaHtmlMultiModule {
  outputDirectory.set(buildDir.resolve("docs/kdoc"))
}

setupAwsPublishingForAllProjects()
setupBintrayPublishing()
setupBuildDirPublishingForAllProjects()
setupGitlabPublishingForAllProjects("gitlab")
