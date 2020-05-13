package org.openstreetmap.josm.gradle.plugin.task.github

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.openstreetmap.josm.gradle.plugin.config.GithubConfig
import org.openstreetmap.josm.gradle.plugin.testutils.buildGithubConfig
import org.openstreetmap.josm.gradle.plugin.testutils.toGradleBuildscript
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver

class PublishToGithubReleaseTaskTest : BaseGithubReleaseTaskTest() {

  private fun BuildResult.assertMessageInOutput(message: String) {
    val pattern = Regex(message, RegexOption.MULTILINE)
    assertTrue(pattern.containsMatchIn(this.output), "Pattern $message not found in:\n${this.output}")
  }

  /**
   * runs a build with custom PublishToGithubReleaseTask
   *  - with parameters supplied as task configuration
   *  - for a custom remote jar name
   */
  @Test
  @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
  fun testCustomPublishTask(
      @WiremockResolver.Wiremock server: WireMockServer,
      @WiremockUriResolver.WiremockUri uri: String
  ) {

    val minJosmVersion = 1111
    val releaseId = 12345678
    val releaseLabel = "v0.0.1"

    val githubConfig = project.buildGithubConfig(uri, GITHUB_USER, "repo_xy", "asdfalkasdhf")

    val localJarName = "test-$releaseLabel.jar"
    val remoteJarName = "test.jar"

    buildFile.writeText("""
      plugins {
          id("org.openstreetmap.josm")
          id("java")
      }
      project.version = "$releaseLabel"
      jar.archiveName = "$localJarName"
      josm {
        ${githubConfig.toGradleBuildscript()}
        josmCompileVersion = "latest"
        manifest {
          description = 'test'
          minJosmVersion = $minJosmVersion
          mainClass = 'test_plugin.TestPlugin'
        }
      }

      task myPublishToGithubRelease(type: ${PublishToGithubReleaseTask::class.qualifiedName}){
        releaseLabel = "$releaseLabel"
        remoteJarName = "$remoteJarName"
      }
      """.trimIndent()
    )

    releaseFile.writeText("""
      releases:
        - label: $releaseLabel
          minJosmVersion: $minJosmVersion
          description: $releaseLabel-description
      """.trimIndent()
    )

    fun prepareAPIStub() {
      // stub for "get releases"
      val path1 = "/repos/$GITHUB_USER/${githubConfig.repositoryName}/releases"
      server.stubFor(
        get(urlPathEqualTo(path1))
          .inScenario("upload-non-existing-asset")
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody("""[{"id": $releaseId, "tag_name": "$releaseLabel"}]""")
          )
      )

      // stub get release assets
      val path2 = "/repos/$GITHUB_USER/${githubConfig.repositoryName}/releases/$releaseId/assets"
      server.stubFor(
        get(urlPathEqualTo(path2))
          .inScenario("upload-non-existing-asset")
          .willReturn(
            aResponse()
              .withStatus(200)
              // no assets
              .withBody("[]")
          )
      )

      // stub for "upload release asset"
      val path3 = "/repos/$GITHUB_USER/${githubConfig.repositoryName}/releases/$releaseId/assets"
      server.stubFor(
        post(urlPathEqualTo(path3))
          .inScenario("upload-non-existing-asset")
          .withHeader("Content-Type", equalTo(MEDIA_TYPE_JAR))
          .withQueryParam("name", equalTo(remoteJarName))
          //.withQueryParam("label", equalTo("test_plugin.jar"))
          .willReturn(
            aResponse()
              .withStatus(201)
              .withBody("""{"id": 1}""")
          )
      )
    }

    prepareTestPluginSource()
    prepareAPIStub()

    val result = GradleRunner.create()
      .withProjectDir(projectDir)
      .withArguments(
        "-P${GithubConfig.PROPERTY_ACCESS_TOKEN}=${githubConfig.accessToken}",
        "--stacktrace",
        "build",
        "myPublishToGithubRelease"
      )
      .withPluginClasspath()
      .build()
    result.dumpOutputOnError("myPublishToGithubRelease")
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":myPublishToGithubRelease")?.outcome
    )
    result.assertMessageInOutput(
      "Uploaded '$localJarName' to release '$releaseLabel' with asset name '$remoteJarName'."
    )
  }

  /**
   * Publish a plugin jar to a normal release using the standard task
   * and standard task configuration. Supply the release label on the
   * command line using '--release-label'.
   */
  @Test
  @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
  fun testDefaultPublishTask(
    @WiremockResolver.Wiremock server: WireMockServer,
    @WiremockUriResolver.WiremockUri uri: String) {

    val minJosmVersion = 1111
    val releaseId = 12345678
    val releaseLabel = "v0.0.1"

    val githubConfig = project.buildGithubConfig(uri, GITHUB_USER, "repo_xy", "asdfalkasdhf")

    // the standard pattern for the jar name the gradle-josm-plugin sets
    // for the plugin jar
    val localJarName = "${projectDir.name}-$releaseLabel.jar"

    buildFile.writeText("""
      plugins {
        id("org.openstreetmap.josm")
        id("java")
      }
      project.version = "$releaseLabel"
      josm {
        ${githubConfig.toGradleBuildscript()}
        josmCompileVersion = "latest"
        manifest {
          description = 'test'
          minJosmVersion = $minJosmVersion
          mainClass = 'test_plugin.TestPlugin'
        }
      }
      // will invoke the standard task 'publishToGithubRelease'
      """.trimIndent()
    )

    releaseFile.writeText("""
      releases:
        - label: $releaseLabel
          minJosmVersion: $minJosmVersion
      """.trimIndent()
    )



    fun prepareAPIStub() {
      // stub for "get releases"
      val leadingPath = "/repos/$GITHUB_USER/${githubConfig.repositoryName}/releases"
      server.stubFor(
        get(urlPathEqualTo(leadingPath))
          .inScenario("upload-asset")
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(
                """
                [{
                  "id": $releaseId,
                  "tag_name": "$releaseLabel"
                }]
                """.trimIndent()
              )
          )
      )

      // stub for get release assets for normal release
      val path2 = "$leadingPath/$releaseId/assets"
      server.stubFor(
        get(urlPathEqualTo(path2))
          .inScenario("upload-assets")
          .willReturn(
            aResponse()
              .withStatus(200)
              // no assets
              .withBody("[]")
          )
      )

      // stub for "upload release asset"
      val path3 = "$leadingPath/$releaseId/assets"
      server.stubFor(
        post(urlPathEqualTo(path3))
          .inScenario("upload-assets")
          .withHeader("Content-Type", equalTo(MEDIA_TYPE_JAR))
          .withQueryParam("name", equalTo(localJarName))
          .willReturn(
            aResponse()
              .withStatus(201)
              .withBody("""{"id": 1}""")
          )
      )
    }

    prepareTestPluginSource()
    prepareAPIStub()

    val result = GradleRunner.create()
      .withProjectDir(projectDir)
      .withArguments(
        "-P${GithubConfig.PROPERTY_ACCESS_TOKEN}=${githubConfig.accessToken}",
        "--stacktrace",
        "build",
        "publishToGithubRelease",
        "--release-label", releaseLabel
      )
      .withPluginClasspath()
      .build()

    result.dumpOutputOnError("publishToGithubRelease")
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":publishToGithubRelease")?.outcome
    )
    result.assertMessageInOutput(
      "Uploaded '$localJarName' to release '$releaseLabel' with asset name '$localJarName'."
    )
  }
}
