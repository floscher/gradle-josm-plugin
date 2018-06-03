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
}
