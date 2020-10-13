plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("org.jetbrains.dokka")
  `maven-publish`
}

dependencies {
  implementation(gradleApi())
  api("org.eclipse.jgit:org.eclipse.jgit:${Versions.jgit}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinSerialization}")
}

publishing {
  publications.create("dogfood", MavenPublication::class) {
    from(components.getByName("java"))
  }
}
