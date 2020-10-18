import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.dokka")
  jacoco
  `maven-publish`
}

kotlin {
  explicitApiWarning()
  js().browser()
  jvm()
}

dependencies {
  add("jvmTestImplementation", "org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
  add("jvmTestRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
}

val javadocJar by tasks.creating(Jar::class)

val jacocoTestReport by tasks.registering(JacocoReport::class) {
  group = "Verification"

  additionalSourceDirs(kotlin.sourceSets.getByName("jvmMain").kotlin.sourceDirectories)
  additionalClassDirs(tasks.getByName<KotlinCompile>("compileKotlinJvm").destinationDir)
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
