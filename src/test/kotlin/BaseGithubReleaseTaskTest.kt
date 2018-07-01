package org.openstreetmap.josm.gradle.plugin

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import java.io.File

open class BaseGithubReleaseTaskTest() {
    protected var buildFile: File? = null
    protected var buildDir: File? = null

    @BeforeEach
    fun setup() {
        buildDir = createTempDir(prefix = "/tmp")
        println("build dir: $buildDir")
        buildFile = File(buildDir, "build.gradle")
        File(buildDir, "settings.gradle").printWriter().use {
            it.println("""
                pluginManagement {
                  repositories {
                      maven {
                        url "${System.getenv("HOME")}/.m2/repository"
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
        val versionProcess = ProcessBuilder().command("git", "describe",
            "--dirty", "--always").start()
        versionProcess.waitFor()
        if (versionProcess.exitValue() != 0) {
            throw Exception("Failed to determine version!")
        }
        val tmpVersion = versionProcess.inputStream.bufferedReader()
            .readText().trim()
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

    protected fun prepareBuildFile(content: String) {
        buildFile?.printWriter()?.use {
            it.println(content)
        }
    }

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
}
