package org.openstreetmap.josm.gradle.plugin.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openstreetmap.josm.gradle.plugin.testutils.GradleProjectUtil
import java.io.File

class I18nConfigTest {
  @Test
  fun testPathTransformer() {
    val project = GradleProjectUtil.createEmptyProjectBuilder().withProjectDir(File(".").absoluteFile).build()
    val transformer = I18nConfig(project).getPathTransformer("gitlab.com/user/repo/blob")
    assertEquals("/some/path", transformer.invoke("/some/path"))
    assertEquals("/some/path:42", transformer.invoke("/some/path:42"))
    assertMatches(transformer.invoke(project.projectDir.absolutePath + "/path/in/project"), Regex("gitlab\\.com/user/repo/blob/[0-9a-f]{4,40}/path/in/project"))
    assertMatches(transformer.invoke(project.projectDir.absolutePath + "/path/in/project:42"), Regex("gitlab\\.com/user/repo/blob/[0-9a-f]{4,40}/path/in/project#L42"))
  }

  private fun assertMatches(string: String, regex: Regex) =
    assertTrue(string.matches(regex), "$string does not match ${regex.pattern}!")
}
