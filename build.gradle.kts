import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openstreetmap.josm.gradle.plugin.GitDescriber
import org.openstreetmap.josm.gradle.plugin.api.gitlab.gitlabRepository
import org.openstreetmap.josm.gradle.plugin.logCoverage
import org.openstreetmap.josm.gradle.plugin.logPublishedMavenArtifacts
import org.openstreetmap.josm.gradle.plugin.logSkippedTasks
import org.openstreetmap.josm.gradle.plugin.logTaskDuration
import java.net.URL
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

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
    additionalSourceDirs(project(":i18n").extensions.getByType(KotlinMultiplatformExtension::class).sourceSets.getByName("commonMain").kotlin.sourceDirectories)
    additionalSourceDirs(project(":i18n").extensions.getByType(KotlinMultiplatformExtension::class).sourceSets.getByName("jvmMain").kotlin.sourceDirectories)

    sourceSets(*
      setOf(":dogfood", ":langconv", ":plugin")
        .map { project(it).extensions.getByName<SourceSetContainer>("sourceSets").getByName("main") }
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
  group = "org.openstreetmap.josm"
  version = GitDescriber(rootProject.projectDir).describe(trimLeading = true)

  repositories.jcenter()

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
  tasks.withType(KotlinCompile::class).all {
    kotlinOptions {
      jvmTarget = javaVersion.toString()
      freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
  }
  tasks.withType(DokkaTask::class) {
    dokkaSourceSets.all {
      skipEmptyPackages.set(false)

      if (platform.get() == Platform.jvm) {
        jdkVersion.set(javaVersion.ordinal + 1)
        externalDocumentationLink(URL("https://docs.gradle.org/${gradle.gradleVersion}/javadoc/"))
        externalDocumentationLink(URL("https://docs.groovy-lang.org/next/html/api/"))
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

    extensions.findByType(PublishingExtension::class)?.repositories?.apply {
      maven(rootProject.buildDir.resolve("maven")) {
        name = "buildDir"
      }

      // Create GitLab Maven repository to publish to.
      gitlabRepository("gitlab", logger)

      // Set up AWS
      val awsAccessKeyId: String? = System.getenv("AWS_ACCESS_KEY_ID")
      val awsSecretAccessKey: String? = System.getenv("AWS_SECRET_ACCESS_KEY")
      if (awsAccessKeyId == null || awsSecretAccessKey == null) {
        logger.lifecycle(
          "Note: If you want to be able to publish the plugin to s3://gradle-josm-plugin , set the environment variables AWS_ACCESS_KEY_ID ({} set) and AWS_SECRET_ACCESS_KEY ({} set).",
          if (awsAccessKeyId == null) { "not" } else { "is" },
          if (awsSecretAccessKey == null) { "not" } else { "is" }
        )
      } else {
        maven("s3://gradle-josm-plugin") {
          name = "s3"
          credentials(AwsCredentials::class.java) {
            accessKey = awsAccessKeyId
            secretKey = awsSecretAccessKey
          }
        }
      }
    }
  }
}

tasks.dokkaHtmlMultiModule {
  outputDirectory.set(buildDir.resolve("docs/kdoc"))
}
