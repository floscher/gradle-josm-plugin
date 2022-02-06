import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

public fun KotlinMultiplatformExtension.configureMultiplatformDefaults() {
  explicitApi()
  jvm()
  js().browser {
    testTask {
      useKarma {
        useFirefoxHeadless()
      }
    }
  }
  sourceSets.named("commonTest") {
    it.dependencies {
      implementation(kotlin("test-common"))
      implementation(kotlin("test-annotations-common"))
    }
  }
  sourceSets.named("jsTest") {
    it.dependencies {
      implementation(kotlin("test-js"))
    }
  }
  sourceSets.named("jvmTest") {
    it.dependencies {
      implementation(kotlin("test-junit5"))
      implementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
      runtimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
    }
  }
}
