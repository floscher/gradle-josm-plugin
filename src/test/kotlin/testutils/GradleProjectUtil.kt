package org.openstreetmap.josm.gradle.plugin.testutils

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.testfixtures.ProjectBuilder
import org.openstreetmap.josm.gradle.plugin.JosmPlugin
import java.nio.file.Files

class GradleProjectUtil private constructor() {
  companion object {
    private val userHomeForTests = Files.createTempDirectory(null).toFile()

    fun createEmptyProjectBuilder(): ProjectBuilder = ProjectBuilder.builder().withGradleUserHomeDir(userHomeForTests)

    fun createJosmPluginProjectWithCleanRepos(withRepo: ((RepositoryHandler) -> Unit)? = null): Project =
      createEmptyProjectBuilder().build().also {
        JosmPlugin().apply(it)
        it.repositories.clear()
        withRepo?.invoke(it.repositories)
      }
  }
}
