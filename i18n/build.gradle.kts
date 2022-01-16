import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.jfrog.bintray")
  kotlin("multiplatform")
  id("org.jetbrains.dokka")
  jacoco
  `maven-publish`
}

tasks.withType(DokkaTask::class) {
  this.dokkaSourceSets.forEach {
    it.samples.from("src/commonTest/kotlin")
  }
}

kotlin {
  explicitApi()
  js().browser {
    testTask {
      useKarma {
        useFirefoxHeadless()
      }
    }
  }
  jvm()

  val jsMain by sourceSets.getting {
    dependencies {
      implementation("org.jetbrains.kotlinx:kotlinx-html:${Versions.kotlinxHtml}")
    }
  }
  val commonTest by sourceSets.getting {
    dependencies {
      implementation(kotlin("test-common"))
      implementation(kotlin("test-annotations-common"))
    }
  }
  val jsTest by sourceSets.getting {
    dependencies {
      implementation(kotlin("test-js"))
    }
  }
  val jvmTest by sourceSets.getting {
    dependencies {
      implementation(kotlin("test"))
      implementation(kotlin("test-junit5"))
      implementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
      runtimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
    }
  }
}

val javadocJar by tasks.creating(Jar::class)

val jacocoTestReport by tasks.registering(JacocoReport::class) {
  group = "Verification"

  additionalSourceDirs(kotlin.sourceSets.getByName("jvmMain").kotlin.sourceDirectories)
  additionalClassDirs(tasks.getByName<KotlinCompile>("compileKotlinJvm").destinationDirectory.asFile.get())
  executionData(buildDir.resolve("jacoco/jvmTest.exec"))
}

publishing {
  publications {
    named<MavenPublication>("jvm") {
      artifact(javadocJar.archiveFile) {
        classifier = "javadoc"
      }
    }
  }
}
