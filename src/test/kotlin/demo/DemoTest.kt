package org.openstreetmap.josm.gradle.plugin.demo

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.openstreetmap.josm.gradle.plugin.testutils.GradleProjectUtil
import java.io.File

class DemoTest {

  @Suppress("unused")
  enum class GradleVersion(val version: String, val expectingSuccess: Boolean) {
    GRADLE_5_4_1("5.4.1", true),
    GRADLE_5_5_1("5.5.1", true),
    GRADLE_5_6_4("5.6.4", true),
    GRADLE_6_0("6.0", false),
    GRADLE_6_0_1("6.0.1", false),
    GRADLE_6_1("6.1", false),
    GRADLE_6_1_1("6.1.1", false),
    GRADLE_6_2("6.2", false),
    GRADLE_6_2_1("6.2.1", false),
    GRADLE_6_2_2("6.2.2", false),
  }

  @ParameterizedTest
  @EnumSource(GradleVersion::class)
  fun testDemo(gradleVersion: GradleVersion, testInfo: TestInfo) {
    println("Building demo project with Gradle ${gradleVersion.version}.")
    println("Expecting to ${if (gradleVersion.expectingSuccess) "succeed" else "fail"}!")
    testDemo(testInfo, GradleRunner.create().withGradleVersion(gradleVersion.version), gradleVersion.expectingSuccess)
  }

  /**
   * Tests if the
   * This test is optional and only run when using one of these command lines:
   * ```
   *  # Runs all tests including this one
   *  DEMO_TEST_CURRENT=true ./gradlew :test
   *  # Runs only this test case
   *  DEMO_TEST_CURRENT=true ./gradlew :test --tests org.openstreetmap.josm.gradle.plugin.demo.DemoTest.testCurrent
   * ```
   */
  @Test
  fun testCurrent(testInfo: TestInfo) {
    // Only execute this test with the current state of the gradle-josm-plugin when this environment variable is set
    assumeTrue { System.getenv().containsKey("DEMO_TEST_CURRENT") }
    println("Building demo project with current (unpublished) version of gradle-josm-plugin.\nExpecting to succeed!")
    testDemo(testInfo, GradleRunner.create(), true, mapOf("settings.gradle.kts" to
      """
      // Allow to test out the demo project with an unpublished version of the plugin in the Maven repo inside the build directory
      pluginManagement.repositories {
        maven(File("${File(".").canonicalPath}/build/maven"))
        gradlePluginPortal()
      }
      // Override the plugin version with the version published in the Maven repo inside the build directory
      pluginManagement {
        resolutionStrategy {
          eachPlugin {
            if (requested.id.id == "org.openstreetmap.josm") {
              this.useVersion(File("${File(".").canonicalPath}/build/maven/org/openstreetmap/josm/gradle-josm-plugin").listFiles()!!.singleOrNull { it.isDirectory }!!.name)
            }
          }
        }
      }
      """.trimIndent()))
  }

  private fun testDemo(testInfo: TestInfo, runner: GradleRunner, expectingSuccess: Boolean, fileContents: Map<String, String> = mapOf()) {
    val tmpDir = GradleProjectUtil.createTempSubDir(testInfo, true)
    println("build dir: ${tmpDir.absolutePath}")

    File("demo").copyRecursively(tmpDir)
    fileContents.forEach { (name, content) ->
      File(tmpDir, name).also { it.parentFile.mkdirs() }.writeText(content)
    }

    runner
      .withProjectDir(tmpDir)
      .withArguments(
        "--stacktrace",
        "build",
        "compileJava_minJosm",
        "compileJava_testedJosm",
        "compileJava_latestJosm",
        "generatePot",
        "localDist",
        "shortenPoFiles",
        "listJosmVersions"
      )
      .forwardOutput()
      .apply {
        if (expectingSuccess) build() else buildAndFail()
      }
  }
}
