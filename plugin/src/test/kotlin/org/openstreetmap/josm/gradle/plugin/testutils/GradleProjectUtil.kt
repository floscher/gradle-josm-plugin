package org.openstreetmap.josm.gradle.plugin.testutils

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.TestInfo
import org.openstreetmap.josm.gradle.plugin.JosmPlugin
import org.openstreetmap.josm.gradle.plugin.util.josm
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

@OptIn(ExperimentalPathApi::class)
class GradleProjectUtil private constructor() {
  companion object {
    private val tmpRootDir: Path by lazy { createTempDirectory("gradle-josm-plugin_unit-test").also { it.toFile().deleteOnExit() } }

    private val gradleUserHome: File by lazy { tmpRootDir.toFile().resolve("gradleUserHome") }

    /**
     * Create a temporary directory below [tmpRootDir]
     * @param testInfo info about the test case for which the directory is created
     * @param initGit if this is `true`, a new git repository is initialized with default user John Doe <john@example.org>
     * @return the directory that was created as a [File]
     */
    fun createTempSubDir(testInfo: TestInfo, initGit: Boolean = false): File =
      createTempDirectory(tmpRootDir, testInfo.toDirString() + '_').toFile().also {
        if (initGit) {
          val git = Git.init().setDirectory(it).call()
          val author = PersonIdent("John Doe", "john@example.org", 0.toLong(), 0)
          git.commit().setAllowEmpty(true).setAuthor(author).setCommitter(author).setMessage("Initial commit").call()
        }
      }

    /**
     * Creates a new [ProjectBuilder] backed by a temporary directory below [tmpRootDir]
     * @param testInfo info about the test case for which the project is created
     * @param initGit if this is `true`, a new git repository is initialized
     * @return the resulting [ProjectBuilder]
     */
    fun createEmptyProjectBuilder(testInfo: TestInfo, initGit: Boolean = false): ProjectBuilder {
      return ProjectBuilder.builder()
        .withProjectDir(createTempSubDir(testInfo, initGit))
        .withGradleUserHomeDir(gradleUserHome)
    }

    private fun TestInfo.toDirString() = testClass.get().simpleName + (testMethod.orElse(null)?.name?.let { "_${it}" } ?: "")

    @ExperimentalUnsignedTypes
    fun createJosmPluginProjectWithCleanRepos(testInfo: TestInfo, initGit: Boolean = false, withRepo: (RepositoryHandler) -> Unit = { }): Project =
      createEmptyProjectBuilder(testInfo, initGit).build().also {
        JosmPlugin().apply(it)
        it.repositories.clear()
        it.extensions.josm.repositories = {}
        withRepo.invoke(it.repositories)
      }

    fun createTempFile(testInfo: TestInfo, suffix: String): File =
      createTempFile(tmpRootDir, testInfo.toDirString() + '_', suffix).toFile()
  }
}
