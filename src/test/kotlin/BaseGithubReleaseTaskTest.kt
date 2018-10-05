package org.openstreetmap.josm.gradle.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.File

const val GITHUB_USER = "a-github-user"

open class BaseGithubReleaseTaskTest() {
    protected var buildFile: File? = null
    protected var buildDir: File? = null

    @BeforeEach
    fun setup() {
        buildDir = createTempDir(prefix = "gradle-josm-plugin_unit-test")
        println("build dir: $buildDir")
        buildFile = File(buildDir, "build.gradle")
        File(buildDir, "settings.gradle").printWriter().use {
            it.println("""
                pluginManagement {
                  repositories {
                      maven {
                        //url "${System.getenv("HOME")}/.m2/repository"
                        url "${File(".").absolutePath}/build/maven"
                      }
                      gradlePluginPortal()
                  }
                }""".trimIndent()
            )
        }
    }

    @AfterEach
    fun tearDown() {
        //buildDir?.deleteRecursively()
    }

    // copy/paste of the code in build.gradle.kts which assembles the plugin
    // version
    fun  pluginUnderTestVersion(): String {
        val tmpVersion = GitDescriber(File(".")).describe()
        return if (tmpVersion[0] == 'v') tmpVersion.substring(1) else tmpVersion
    }

    protected fun prepareTestPluginSource() {
        val testPluginContent = """
                package test_plugin;
                import org.openstreetmap.josm.plugins.Plugin;
                import org.openstreetmap.josm.plugins.PluginInformation;
                public class TestPlugin extends Plugin {
                  public TestPlugin(PluginInformation info) {
                      super(info);
                  }
                }
            """.trimIndent()
        val sourceDir = File(buildDir, "src/main/java/test_plugin")
        sourceDir.mkdirs()

        val sourceFile = File(sourceDir, "TestPlugin.java")
        sourceFile.printWriter().use { out ->
            out.println(testPluginContent)
        }
    }

    protected fun prepareBuildFile(content: String) = buildFile!!.writeText(content)

    protected fun prepareReleasesSpecs(content: String,
                                       releasesFile: File? = null) {
        (releasesFile ?: File(buildDir, "releases.yml"))
            .printWriter().use { it.println(content) }
    }

    protected fun prepareGradleProperties(content: String,
                                          propertiesFile: File? = null) {
        (propertiesFile ?: File(buildDir, "gradle.properties"))
            .printWriter().use { it.println(content) }
    }

    protected fun BuildResult.dumpOutputOnError(taskName: String) {
        if (this.task(":$taskName")?.outcome != SUCCESS) {
            println(this.output)
        }
    }
}
