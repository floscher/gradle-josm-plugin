package org.openstreetmap.josm.gradle.plugin.config

import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.openstreetmap.josm.gradle.plugin.testutils.GradleProjectUtil

class I18nConfigTest {
  @Test
  fun testPathTransformer(testInfo: TestInfo) {
    val project = GradleProjectUtil.createEmptyProjectBuilder(testInfo, true).build()
    val gitCommitHash = Git.open(project.projectDir).repository.refDatabase.exactRef("HEAD").objectId.name
    val transformer = I18nConfig().getPathTransformer(project.projectDir, "gitlab.com/user/repo/blob")
    assertEquals("/some/path", transformer.invoke("/some/path"))
    assertEquals("/some/path:42", transformer.invoke("/some/path:42"))
    assertEquals(transformer.invoke(project.projectDir.absolutePath + "/path/in/project"), "gitlab.com/user/repo/blob/${gitCommitHash.substring(0 until 7)}/path/in/project")
    assertEquals(transformer.invoke(project.projectDir.absolutePath + "/path/in/project:42"), "gitlab.com/user/repo/blob/${gitCommitHash.substring(0 until 7)}/path/in/project#L42")
  }
}
