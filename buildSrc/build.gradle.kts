plugins {
  kotlin("jvm").version(Versions.kotlin)
  kotlin("plugin.serialization").version(Versions.kotlin)
  `java-library`
}

repositories {
  jcenter()
}

val dogfood by sourceSets.registering

kotlin.sourceSets.named(dogfood.name) {
  kotlin.setSrcDirs(setOf(File(projectDir, "../src/dogfood/kotlin/")))
}

dependencies {
  implementation(dogfood.get().output)
  runtimeOnly(dogfood.get().runtimeClasspath)

  add(dogfood.get().implementationConfigurationName, gradleApi())
  add(dogfood.get().implementationConfigurationName, "org.eclipse.jgit:org.eclipse.jgit:${Versions.jgit}")
  add(dogfood.get().implementationConfigurationName, kotlin("stdlib", Versions.kotlin))
  add(dogfood.get().implementationConfigurationName, (dependencies.create("org.jetbrains.kotlinx:kotlinx-serialization-runtime:${Versions.kotlinSerialization}") as ModuleDependency).also {
    it.exclude("org.jetbrains.kotlinx")
  })
}
