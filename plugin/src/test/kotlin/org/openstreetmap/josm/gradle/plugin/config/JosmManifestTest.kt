package org.openstreetmap.josm.gradle.plugin.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertThrows
import org.openstreetmap.josm.gradle.plugin.task.CompileToLang
import org.openstreetmap.josm.gradle.plugin.task.GenerateJarManifest
import org.openstreetmap.josm.gradle.plugin.testutils.GradleProjectUtil

class JosmManifestTest {

  @Test
  public fun testAddClasspath(testInfo: TestInfo) {
    val manifest = createTestManifest(testInfo)
    assertEquals(listOf<String>(), manifest.classpath)
    manifest.classpath("A")
    assertEquals(listOf("A"), manifest.classpath)
    manifest.classpath("B", "C", "D", "E", "F")
    assertEquals(listOf("A", "B", "C", "D", "E", "F"), manifest.classpath)
  }

  @Test
  public fun testClasspathWithWhitespace(testInfo: TestInfo) {
    val manifest = createTestManifest(testInfo)
    assertThrows<IllegalArgumentException> {
      manifest.classpath("")
    }
    assertThrows<IllegalArgumentException> {
      manifest.classpath(" ")
    }
    assertThrows<IllegalArgumentException> {
      manifest.classpath("A\tB")
    }
    assertThrows<IllegalArgumentException> {
      manifest.classpath("bli", "bla", "blub\rb")
    }
  }

  private fun createTestManifest(testInfo: TestInfo): JosmManifest {
    val project = GradleProjectUtil.createEmptyProjectBuilder(testInfo, false).build()
    return JosmManifest(project)
  }
}
