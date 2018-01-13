package org.openstreetmap.josm.gradle.plugin.config

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class I18nConfigTest {
  @Test
  fun testGithubTransformer() {
    val project = ProjectBuilder.builder().build()
    val transformer = I18nConfig(project).getGithubPathTransformer("user/repo")
    assertEquals("/some/path", transformer.invoke("/some/path"))
    assertEquals("/some/path:42", transformer.invoke("/some/path:42"))
    assertTrue(transformer.invoke(project.projectDir.absolutePath + "/path/in/project").matches(Regex("github\\.com/user/repo/blob/[0-9a-f]{4,40}/path/in/project")))
    assertTrue(transformer.invoke(project.projectDir.absolutePath + "/path/in/project:42").matches(Regex("github\\.com/user/repo/blob/[0-9a-f]{4,40}/path/in/project#L42:42")))
  }
}
