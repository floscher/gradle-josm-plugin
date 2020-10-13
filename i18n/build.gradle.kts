plugins {
  kotlin("jvm")
  id("org.jetbrains.dokka")
  `maven-publish`
}

dependencies {
  testImplementation("org.junit.jupiter", "junit-jupiter-api", Versions.junit)
  testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", Versions.junit)
}

publishing {
  publications.create("i18n", MavenPublication::class) {
    from(components.getByName("java"))
  }
}
