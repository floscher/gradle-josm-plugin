package org.openstreetmap.josm.gradle.plugin.task.github

import org.gradle.api.Project
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.openstreetmap.josm.gradle.plugin.testutils.GradleProjectUtil
import java.io.File


const val GITHUB_USER = "a-github-user"

open class BaseGithubReleaseTaskTest() {
  protected lateinit var projectDir: File
  protected lateinit var project: Project
  protected val buildFile by lazy { File(projectDir, "build.gradle") }
  protected val releaseFile by lazy { File(projectDir, "releases.yml") }

  @BeforeEach
  fun setup(testInfo: TestInfo) {
    GradleProjectUtil.createEmptyProjectBuilder(testInfo).build()
      .also { project = it }
      .projectDir
      .also { projectDir ->
        this.projectDir = projectDir
        println("build dir: $projectDir")
      }
  }


  protected fun prepareTestPluginSource() {
    File(projectDir, "src/main/java/test_plugin/TestPlugin.java").also {
      it.parentFile.mkdirs()
      it.writeText("""
        package test_plugin;
        import org.openstreetmap.josm.plugins.Plugin;
        import org.openstreetmap.josm.plugins.PluginInformation;
        public class TestPlugin extends Plugin {
          public TestPlugin(PluginInformation info) {
              super(info);
          }
        }
        """.trimIndent()
      )
    }
  }

  protected fun BuildResult.dumpOutputOnError(taskName: String) {
    if (this.task(":$taskName")?.outcome != SUCCESS) {
      println(this.output)
    }
  }
}
