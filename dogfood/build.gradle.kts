plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("org.jetbrains.dokka")
  `maven-publish`
}

kotlin {
  explicitApi()
}

dependencies {
  implementation(gradleApi())
  api("org.eclipse.jgit:org.eclipse.jgit:${Versions.jgit}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinSerialization}")
}

publishing {
  publications.create(project.name, MavenPublication::class) {
    from(components.getByName("java"))
  }
}
