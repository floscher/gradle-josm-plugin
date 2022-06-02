import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openstreetmap.josm.gradle.plugin.GitDescriber
import org.openstreetmap.josm.gradle.plugin.api.gitlab.setupGitlabPublishing
import org.openstreetmap.josm.gradle.plugin.logCoverage
import org.openstreetmap.josm.gradle.plugin.logPublishedMavenArtifacts
import org.openstreetmap.josm.gradle.plugin.logSkippedTasks
import org.openstreetmap.josm.gradle.plugin.logTaskDuration
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
    setOf(
      project(":common").tasks.getByName<Test>("jvmTest"),
      project(":i18n").tasks.getByName<Test>("jvmTest"),
      project(":plugin").tasks.getByName<Test>("test")
    ).forEach {
      executionData(it)
      dependsOn(it)
    }

    setOf(":common", ":i18n").map { project(it) }.let { mppProjects ->
      mppProjects.forEach { mppProject ->
        sourceDirectories.from(
          mppProject.extensions.getByType(KotlinMultiplatformExtension::class).sourceSets
            .filter { it.name.endsWith("Main") }
            .map { it.kotlin.sourceDirectories }
        )
      }
      mppProjects.forEach { classDirectories.from(it.layout.buildDirectory.dir("classes/kotlin/jvm")) }
    }

    sourceSets(*
      subprojects
        .mapNotNull { it.extensions.getByType(SourceSetContainer::class).findByName(SourceSet.MAIN_SOURCE_SET_NAME) }
        .toTypedArray()
    )
  }

  val build by tasks.getting {
    dependsOn(tasks.dokkaHtmlMultiModule)
  }
  val check by tasks.getting {
    dependsOn(jacocoTestReport)
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

  repositories.mavenCentral()

  tasks.withType(JacocoReport::class).all {
    tasks.findByName("test")?.let { dependsOn(it) }
    logCoverage()
  }

  tasks.withType(Zip::class).all {
    // make the produces archives (more) reproducible
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
  }
}

subprojects {

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
  tasks.withType(AbstractDokkaLeafTask::class) {
    dokkaSourceSets.all {
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

      file("src/${this@all.name}/kotlin").takeIf { it.exists() }?.let { localSrcDir ->
        sourceLink {
          remoteUrl.set(URL("https://gitlab.com/JOSM/gradle-josm-plugin/-/tree/v${project.version}/${project.name}/src/${this@all.name}/kotlin"))
          localDirectory.set(localSrcDir)
          remoteLineSuffix.set("#L")
        }
      }
    }
  }

  pluginManager.withPlugin("java") {
    extensions.findByType(JavaPluginExtension::class)?.apply {
      sourceCompatibility = javaVersion
      withSourcesJar()
    }
  }

  configureIfMultiplatform { multiplatformExtension ->
    multiplatformExtension.configureMultiplatformDefaults()

    configureIfJacoco {
      tasks.register("jacocoTestReport", JacocoReport::class).configure {
        group = "Verification"
        dependsOn(tasks.getByName("jvmTest"))
        classDirectories.from(layout.buildDirectory.dir("classes/kotlin/jvm"))
        sourceDirectories.from(
          multiplatformExtension.sourceSets
            .filter { it.name.endsWith("Main") }
            .map { it.kotlin.sourceDirectories }
        )
        executionData(tasks.getByName("jvmTest"))
        reports {
          html.required.set(true)
        }
      }
    }
  }

  val javadocJar by project.tasks.registering(Jar::class) {
    group = "Documentation"
    from(project.provider { tasks.named("dokkaHtml") })
    archiveClassifier.set("javadoc")
  }

  configureIfMavenPublishing {
    it.publications {
      withType(MavenPublication::class) {
        artifact(javadocJar.flatMap { it.archiveFile }) {
          classifier = "javadoc"
        }
      }
    }
  }
}

tasks.dokkaHtmlMultiModule {
  outputDirectory.set(buildDir.resolve("docs/kdoc"))
}

allprojects {
  setupBuildDirPublishing()
  setupAwsPublishing()
  setupMavenArtifactSigning()
  setupGitlabPublishing("gitlab")
  setupOssSonatypeStagingPublishing()

  addMavenPomContent(gradleJosmPluginMetadata())
}
