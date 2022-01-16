package org.openstreetmap.josm.gradle.plugin.github

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openstreetmap.josm.gradle.plugin.config.GithubConfig
import org.openstreetmap.josm.gradle.plugin.task.github.BaseGithubReleaseTaskTest
import org.openstreetmap.josm.gradle.plugin.task.github.MEDIA_TYPE_JAR
import org.openstreetmap.josm.gradle.plugin.testutils.buildGithubConfig
import org.openstreetmap.josm.gradle.plugin.testutils.toGradleBuildscript
import java.io.File
import java.util.jar.Attributes
import java.util.jar.JarInputStream

@WireMockTest
class JarWithDownloadLinksTest: BaseGithubReleaseTaskTest() {
    val GITHUB_REPO = "repo_xy"

    @Test
    fun case01(wmRuntimeInfo: WireMockRuntimeInfo) {

        val currentMinJosmVersion = 2222
        val currentRelease = "v0.0.2"

        // prepare test plugin source
        prepareTestPluginSource()

        // prepare releases file
        releaseFile.writeText("""
          releases:
            # an former release. A download link for this release
            # should be included in the Manifest
            - label: v0.0.1
              minJosmVersion: 1111

            # the current release
            - label: $currentRelease
              minJosmVersion: $currentMinJosmVersion
          """.trimIndent()
        )

        val githubConfig = project.buildGithubConfig(wmRuntimeInfo.httpBaseUrl, GITHUB_USER, GITHUB_REPO, "42alsdkj-foiau_osf0123456789")
        // prepare build file
        buildFile.writeText("""
            plugins {
                id("org.openstreetmap.josm")
                id("java")
            }
            version = "$currentRelease"
            jar.archiveName = "test.jar"
            josm {
              ${githubConfig.toGradleBuildscript()}
              josmCompileVersion = "latest"
              manifest {
                  includeLinksToGithubReleases = true
                  description = 'test plugin'
                  minJosmVersion = $currentMinJosmVersion
                  mainClass = 'test_plugin.TestPlugin'
              }
            }
            """
        )

        // prepare API stub

        val path1 = "/repos/$GITHUB_USER/$GITHUB_REPO/releases"
        val assetUrl = "http://a.b.c/$GITHUB_USER/$GITHUB_REPO/download/v0.0.1/test.jar"
        wmRuntimeInfo.wireMock.register(WireMock.get(WireMock.urlPathMatching("$path1.*"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withBody(
                    // reply one release with one relase asset for the old
                    // release 'v0.0.1'
                    """[
                        {"id": 1,
                         "tag_name": "v0.0.1",
                         "assets": [
                            {"id": 1,
                             "browser_download_url": "$assetUrl",
                             "content_type": "$MEDIA_TYPE_JAR"
                            }
                         ]
                         }
                      ]
                    """.trimIndent())
            )
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("--stacktrace", "-P${GithubConfig.PROPERTY_ACCESS_TOKEN}=${githubConfig.accessToken}", "build")
            .withPluginClasspath()
            .build()

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":build")?.outcome
        )

        val jarFile = File(projectDir, "build/libs/test.jar")
        val manifest = JarInputStream(jarFile.inputStream()).manifest

    assertTrue(
      ReleaseSpec.loadListFrom(File(projectDir, "releases.yml").inputStream())
      // don't check for the current release. Because we are building it
      // now, there is no download URL available yet
      .filter {it.label != currentRelease}
      .all { release->
        val josmVersion = release.minJosmVersion
        val key = Attributes.Name("${josmVersion}_Plugin-Url")
        val ret = manifest.mainAttributes.keys.contains(key)
        if (!ret) {
          println("Error: no Plugin-Url for $josmVersion  included in Manifest")
        }
        ret
      }
    )
  }

  @Test
  fun case02(wmRuntimeInfo: WireMockRuntimeInfo) {

    val currentMinJosmVersion = 2222
    val currentRelease = "v0.1.0"

    // prepare test plugin source
    prepareTestPluginSource()

    // prepare releases file
    releaseFile.writeText("""
      releases:
        # an former release. A download link for this release
        # should be included in the Manifest
        - label: v0.0.1
          minJosmVersion: 1111

        # an former release. A download link for this release
        # should be included in the Manifest
        - label: v0.0.2
          minJosmVersion: 1111

        # the current release
        - label: $currentRelease
          minJosmVersion: $currentMinJosmVersion
      """.trimIndent()
    )

    val githubConfig = project.buildGithubConfig(wmRuntimeInfo.httpBaseUrl, GITHUB_USER, GITHUB_REPO, "42alsdkj-foiau_osf0123456789")

    // prepare build file
    buildFile.writeText("""
      plugins {
          id("org.openstreetmap.josm")
          id("java")
      }
      version = "$currentRelease"
      jar.archiveName = "test.jar"
      josm {
        ${githubConfig.toGradleBuildscript()}
        josmCompileVersion = "latest"
        manifest {
            includeLinksToGithubReleases = true
            description = 'test plugin'
            minJosmVersion = $currentMinJosmVersion
            mainClass = 'test_plugin.TestPlugin'
        }
      }
      """.trimIndent()
    )

        // prepare API stub

        val path1 = "/repos/$GITHUB_USER/$GITHUB_REPO/releases"
        fun assetUrl(label: String) =
            "http://a.b.c/$GITHUB_USER/$GITHUB_REPO/download/$label/test.jar"
        wmRuntimeInfo.wireMock.register(WireMock.get(WireMock.urlPathMatching("$path1.*"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withBody(
                    // two releases, with one release asset (the plugin jar)
                    // each
                    """[
                        {"id": 1,
                         "tag_name": "v0.0.1",
                         "assets": [
                            {"id": 1,
                             "browser_download_url": "${assetUrl("v0.0.1")}",
                             "content_type": "$MEDIA_TYPE_JAR"
                            }
                         ]
                         },
                        {"id": 2,
                         "tag_name": "v0.0.2",
                         "assets": [
                            {"id": 1,
                             "browser_download_url": "${assetUrl("v0.0.2")}",
                             "content_type": "$MEDIA_TYPE_JAR"
                            }
                         ]
                         }
                      ]
                    """.trimIndent())
            )
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("-P${GithubConfig.PROPERTY_ACCESS_TOKEN}=${githubConfig.accessToken}", "build")
            .withPluginClasspath()
            .build()

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":build")?.outcome
        )

        val jarFile = File(projectDir, "build/libs/test.jar")
        val manifest = JarInputStream(jarFile.inputStream()).manifest

    assertTrue(
      ReleaseSpec.loadListFrom(File(projectDir, "releases.yml").inputStream())
        // don't check for the current release. Because we are building it
        // now  there is no download URL available yet
        .filter {it.label != currentRelease}
        .all { release->
          val josmVersion = release.minJosmVersion
          val key = Attributes.Name("${josmVersion}_Plugin-Url")
          val ret = manifest.mainAttributes.keys.contains(key)
          if (!ret) {
            println("Error: no Plugin-Url for JOSM version $josmVersion included in Manifest")
          }
          ret
        }
    )
    assertTrue(manifest.mainAttributes.getValue("1111_Plugin-Url").contains("v0.0.2"))
  }


  @Test
  fun case03(wmRuntimeInfo: WireMockRuntimeInfo) {

    val currentMinJosmVersion = 3000
    val currentRelease = "v0.2.0"

    // prepare test plugin source
    prepareTestPluginSource()

    // prepare releases file
    releaseFile.writeText("""
      releases:
        - label: v0.0.1
          minJosmVersion: 1000

        - label: v0.1.1
          minJosmVersion: 2000

        - label: v0.1.2
          minJosmVersion: 2000

        # the current release
        - label: $currentRelease
          minJosmVersion: $currentMinJosmVersion
      """.trimIndent()
    )

        val githubConfig = project.buildGithubConfig(wmRuntimeInfo.httpBaseUrl, GITHUB_USER, GITHUB_REPO, "42alsdkj-foiau_osf0123456789")
        // prepare build file
        buildFile.writeText("""
            plugins {
              id("org.openstreetmap.josm")
              id("java")
            }
            version = "$currentRelease"
            jar.archiveName = "test.jar"
            josm {
              ${githubConfig.toGradleBuildscript()}
              josmCompileVersion = "latest"
              manifest {
                  includeLinksToGithubReleases = true
                  description = 'test plugin'
                  minJosmVersion = $currentMinJosmVersion
                  mainClass = 'test_plugin.TestPlugin'
              }
            }
            """
        )

        // prepare API stub
        val path1 = "/repos/$GITHUB_USER/$GITHUB_REPO/releases"
        fun assetUrl(label: String) =
            "http://a.b.c/$GITHUB_USER/$GITHUB_REPO/download/$label/test.jar"
        wmRuntimeInfo.wireMock.register(WireMock.get(WireMock.urlPathMatching("$path1.*"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withBody(
                    // two releases, with one release asset (the plugin jar)
                    // each
                    """[
                        {"id": 1,
                         "tag_name": "v0.1.2",
                         "assets": [
                            {"id": 1,
                             "browser_download_url": "${assetUrl("v0.1.2")}",
                             "content_type": "$MEDIA_TYPE_JAR"
                            },
                            {"id": 2,
                             "browser_download_url": "http://another.url",
                             "content_type": "text/plain"
                            }
                           ]
                         },
                         {"id": 2,
                          "tag_name": "v0.1.1"
                         },
                         {"id": 3,
                          "tag_name": "v0.0.1",
                          "assets": [
                            {"id": 1,
                             "browser_download_url": "${assetUrl("v0.0.1")}",
                             "content_type": "$MEDIA_TYPE_JAR"
                            }
                           ]
                         }
                      ]
                    """.trimIndent())
            )
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("-P${GithubConfig.PROPERTY_ACCESS_TOKEN}=${githubConfig.accessToken}", "build")
            .withPluginClasspath()
            .build()

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":build")?.outcome
        )
        println(result.output)

        val jarFile = File(projectDir, "build/libs/test.jar")
        val manifest = JarInputStream(jarFile.inputStream()).manifest

        // should include a download URL for release 2000
        var value: String? = manifest.mainAttributes.getValue(
            Attributes.Name("2000_Plugin-Url"))
        assertTrue(value?.contains("v0.1.2") ?: false,
            "2000_Plugin-Url should include 'v0.1.2'")
        // should include a download URL for release 1000
        value = manifest.mainAttributes.getValue(
            Attributes.Name("1000_Plugin-Url"))
        assertTrue(value?.contains("v0.0.1") ?: false,
            "1000_Plugin-Url should contain 'v0.0.1'")
    }
}
