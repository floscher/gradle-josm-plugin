plugins {
  java
}

sourceSets.main {
  java {
    this.setSrcDirs(setOf(projectDir, "../src/main/java"))
  }
}
