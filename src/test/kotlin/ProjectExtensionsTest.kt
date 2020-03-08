package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.openstreetmap.josm.gradle.plugin.testutils.GradleProjectUtil
import org.openstreetmap.josm.gradle.plugin.util.createJosm
import org.openstreetmap.josm.gradle.plugin.util.createJosmDependencyFuzzy
import org.openstreetmap.josm.gradle.plugin.util.excludeJosm
import org.openstreetmap.josm.gradle.plugin.util.getAllRequiredJosmPlugins
import org.openstreetmap.josm.gradle.plugin.util.josm

@ExperimentalUnsignedTypes
class ProjectExtensionsTest {

  private fun createRequiredPluginsTestRepo(testInfo: TestInfo): Project =
    GradleProjectUtil.createJosmPluginProjectWithCleanRepos(testInfo) {
      it.ivy { repo ->
        repo.url = ProjectExtensionsTest::class.java.getResource("/josmPluginRepo").toURI()
        repo.patternLayout {
          it.artifact("[artifact].jar")
        }
        repo.metadataSources {
          it.artifact()
        }
      }
    }

  @Test
  fun testRequiredPlugins(testInfo: TestInfo) {
    val result = createRequiredPluginsTestRepo(testInfo).getAllRequiredJosmPlugins(setOf("A"))
    assertEquals(6, result.size)
    assertEquals(1, result.count{ it.name == "A" })
    assertEquals(1, result.count{ it.name == "B" })
    assertEquals(1, result.count{ it.name == "C" })
    assertEquals(1, result.count{ it.name == "D" })
    assertEquals(1, result.count{ it.name == "E" })
    assertEquals(1, result.count{ it.name == "F" })
  }

  private fun createNextJosmTestRepo(testInfo: TestInfo): Project =
    GradleProjectUtil.createJosmPluginProjectWithCleanRepos(testInfo) {
      it.ivy { repo ->
        repo.url = ProjectExtensionsTest::class.java.getResource("/josmRepo").toURI()
        repo.patternLayout {
          it.artifact("[artifact]-[revision]")
        }
        repo.metadataSources {
          it.artifact()
        }
      }
    }

  @Test
  fun testNextJosm(testInfo: TestInfo) {
    assertEquals("100", createNextJosmTestRepo(testInfo).createJosmDependencyFuzzy(51.toUInt(), 50.toUInt()).version)
    assertEquals("100", createNextJosmTestRepo(testInfo).createJosmDependencyFuzzy(71.toUInt()).version)
  }

  @Test
  fun testNextJosmFail(testInfo: TestInfo) {
    assertThrows(GradleException::class.java) {
      createNextJosmTestRepo(testInfo).createJosmDependencyFuzzy(50.toUInt(), 50.toUInt())
    }
    assertThrows(GradleException::class.java) {
      createNextJosmTestRepo(testInfo).createJosmDependencyFuzzy(70.toUInt())
    }
  }

  @Test
  fun testNextJosmSame(testInfo: TestInfo) {
    assertEquals("100", createNextJosmTestRepo(testInfo).createJosmDependencyFuzzy(100.toUInt()).version)
  }

  @Test
  fun testArbitraryJosmString(testInfo: TestInfo) {
    val project = createNextJosmTestRepo(testInfo)
    val josmDep = project.dependencies.createJosm("XYZ")
    val conf = project.configurations.detachedConfiguration(josmDep)
    conf.excludeJosm()
    assertTrue(conf.excludeRules.all { it.group == josmDep.group && it.module == josmDep.name })
    assertEquals("XYZ", josmDep.version)
    assertEquals(false, josmDep.isChanging)
    project.configurations.detachedConfiguration(josmDep).resolve()
  }

  @Test
  fun testNextJosmStringFail(testInfo: TestInfo) {
    assertThrows(GradleException::class.java) {
      val project = createNextJosmTestRepo(testInfo)
      val josmDep = project.dependencies.createJosm("ABC")
      project.configurations.detachedConfiguration(josmDep).resolve()
    }
  }

  @Test
  fun testJosmExtension(testInfo: TestInfo) {
    GradleProjectUtil.createJosmPluginProjectWithCleanRepos(testInfo).extensions.josm
  }
}
