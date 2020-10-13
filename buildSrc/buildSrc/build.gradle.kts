plugins {
  java
}

sourceSets.main {
  java {
    setSrcDirs(setOf(projectDir.parentFile.resolve("src/main/java")))
  }
}
