package org.openstreetmap.josm.gradle.plugin.task.github

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openstreetmap.josm.gradle.plugin.config.GithubConfig
import org.openstreetmap.josm.gradle.plugin.testutils.buildGithubConfig
import org.openstreetmap.josm.gradle.plugin.testutils.toGradleBuildscript
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_OK

@WireMockTest
class CreateGithubReleaseTaskTest: BaseGithubReleaseTaskTest() {

    /**
     * Can create a release
     * - for a custom release label
     * - configured in a custom task in the build file
     */
    @Test
    fun test01(wmRuntimeInfo: WireMockRuntimeInfo) {

        val releaseName = "a test release"
        val releaseLabel = "v0.0.2"

        releaseFile.writeText("""
          releases:
            - label: "$releaseLabel"
              name: "$releaseName"
              minJosmVersion: 5678
              description: a test description
          """.trimIndent()
        )

        val githubConfig = project.buildGithubConfig(wmRuntimeInfo.httpBaseUrl, "github_user", "repo", "aaaabbbb")

        buildFile.writeText("""
            plugins {
                id("org.openstreetmap.josm")
            }
            project.version="v0.0.1"
            josm {
                ${githubConfig.toGradleBuildscript()}
                josmCompileVersion = 14002
                manifest {
                    minJosmVersion = 14002
                    mainClass = "test_plugin.TestPlugin"
                }
            }
            task myCreateGithubRelease(type: ${CreateGithubReleaseTask::class.qualifiedName}){
              releaseLabel = "$releaseLabel"
            }
            """.trimIndent()
        )

        val path = "/repos/${githubConfig.repositoryOwner}/${githubConfig.repositoryName}/releases"

        wmRuntimeInfo.wireMock.register(WireMock.get(WireMock.urlPathEqualTo(path))
            .withBasicAuth(githubConfig.repositoryOwner, githubConfig.accessToken)
            .willReturn(WireMock.aResponse()
            .withStatus(200)
            // assume we already have one release with label 'v0.0.1'
            // on the github server
            .withBody("""[{"id": 1, "label": "v0.0.1"}]""")
        ))

        wmRuntimeInfo.wireMock.register(WireMock.post(WireMock.urlPathEqualTo(path))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.tag_name == '$releaseLabel')]"))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.name == '$releaseName')]"))
            .withBasicAuth(githubConfig.repositoryOwner, githubConfig.accessToken)
            .willReturn(WireMock.aResponse()
            .withStatus(HTTP_CREATED)
            .withBody("""{"id": 1}""")
        ))

        val result = GradleRunner.create()
          .withProjectDir(projectDir)
          .withArguments(
            "-P${GithubConfig.PROPERTY_ACCESS_TOKEN}=${githubConfig.accessToken}",
            "myCreateGithubRelease",
            "--stacktrace"
          )
          .withPluginClasspath()
          .build()
        result.dumpOutputOnError("myCreateGithubRelease")
        assertEquals(SUCCESS, result.task(":myCreateGithubRelease")?.outcome)
    }

    /**
     * Can create a release
     * - for a custom release label
     * - configured in the default task in the build file
     */
    @Test
    fun test02(wmRuntimeInfo: WireMockRuntimeInfo) {

        val releaseName = "a test release"
        val releaseLabel = "v0.0.2"

        releaseFile.writeText("""
          releases:
            - label: "$releaseLabel"
              name: "$releaseName"
              minJosmVersion: 5678
              description: a test description
          """.trimIndent()
        )

        val githubConfig = project.buildGithubConfig(wmRuntimeInfo.httpBaseUrl, "JOSM", "some-repo", "abcdefghijklmnopqrstuvwxyz")

        buildFile.writeText("""
            plugins {
                id("org.openstreetmap.josm")
            }
            project.version="v0.0.1"
            josm {
                ${githubConfig.toGradleBuildscript()}
                josmCompileVersion = 14002
                manifest {
                    minJosmVersion = 14002
                    mainClass = "test_plugin.TestPlugin"
                }
            }
            createGithubRelease {
              releaseLabel = "$releaseLabel"
            }
            """.trimIndent()
        )

        val path = "/repos/${githubConfig.repositoryOwner}/${githubConfig.repositoryName}/releases"

        wmRuntimeInfo.wireMock.register(WireMock.get(WireMock.urlPathEqualTo(path))
            .withBasicAuth(githubConfig.repositoryOwner, githubConfig.accessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(HTTP_OK)
                // assume we already have one release with label 'v0.0.1'
                // on the github server
                .withBody("""[{"id": 1, "label": "v0.0.1"}]""")
            ))

        wmRuntimeInfo.wireMock.register(WireMock.post(WireMock.urlPathEqualTo(path))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.tag_name == '$releaseLabel')]"))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.name == '$releaseName')]"))
            .withBasicAuth(githubConfig.repositoryOwner, githubConfig.accessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(HTTP_CREATED)
                .withBody("""{"id": 1}""")
            ))

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(
              "-P${GithubConfig.PROPERTY_ACCESS_TOKEN}=${githubConfig.accessToken}",
              "createGithubRelease",
              "--stacktrace"
            )
            .withPluginClasspath()
            .build()

        result.dumpOutputOnError("createGithubRelease")
        assertEquals(SUCCESS, result.task(":createGithubRelease")?.outcome)
    }

    /**
     * Can create a release
     * - for a custom label
     * - configured in the standard task
     * - when the repo name is not explicitly specified
     */
    @Test
    fun test03(wmRuntimeInfo: WireMockRuntimeInfo) {
        val releaseLabel = "v0.0.2"

        releaseFile.writeText("""
          releases:
            - label: "$releaseLabel"
              minJosmVersion: 5678
          """.trimIndent()
        )

        val githubConfig = project.buildGithubConfig(wmRuntimeInfo.httpBaseUrl, "github_user", "repoName", "aaaabbbb")

        buildFile.writeText("""
            plugins {
                id("org.openstreetmap.josm")
            }

            project.version="v0.0.1"
            josm {
                ${githubConfig.toGradleBuildscript()}
                josmCompileVersion = 14002
                manifest {
                    minJosmVersion = 14002
                    mainClass = "test_plugin.TestPlugin"
                }
            }

            createGithubRelease {
              releaseLabel = "$releaseLabel"
            }
            """.trimIndent()
        )

        val path = "/repos/${githubConfig.repositoryOwner}/${githubConfig.repositoryName}/releases"

        wmRuntimeInfo.wireMock.register(WireMock.get(WireMock.urlPathEqualTo(path))
            .withBasicAuth(githubConfig.repositoryOwner, githubConfig.accessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(HTTP_OK)
                // assume we only have one release with label 'v0.0.1'
                .withBody("""[{"id": 1, "label": "v0.0.1"}]""")
            ))

        wmRuntimeInfo.wireMock.register(WireMock.post(WireMock.urlPathEqualTo(path))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.tag_name == '$releaseLabel')]"))
            .withBasicAuth(githubConfig.repositoryOwner, githubConfig.accessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(HTTP_CREATED)
                .withBody("""{"id": 1}""")
            ))


        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(
              "-P${GithubConfig.PROPERTY_ACCESS_TOKEN}=${githubConfig.accessToken}",
              "createGithubRelease",
              "--stacktrace"
            )
            .withPluginClasspath()
            .build()
        result.dumpOutputOnError("createGithubRelease")
        assertEquals(SUCCESS, result.task(":createGithubRelease")?.outcome)
    }

    /**
     * Can create a release
     * - for a custom release label
     * - configured with the command line option --release-label
     */
    @Test
    fun test04(wmRuntimeInfo: WireMockRuntimeInfo) {

        val releaseName = "a test release"
        val releaseLabel = "v0.0.2"

        releaseFile.writeText("""
          releases:
            - label: "$releaseLabel"
              name: "$releaseName"
              minJosmVersion: 5678
              description: a test description
          """.trimIndent()
        )

        val githubConfig = project.buildGithubConfig(wmRuntimeInfo.httpBaseUrl, "github_user", "repo", "aaaabbbb")

        buildFile.writeText("""
            plugins {
                id("org.openstreetmap.josm")
            }
            project.version="v0.0.1"
            josm {
                ${githubConfig.toGradleBuildscript()}
                josmCompileVersion = 14002
                manifest {
                    minJosmVersion = 14002
                    mainClass = "test_plugin.TestPlugin"
                }
            }
            """.trimIndent()
        )

        val path = "/repos/${githubConfig.repositoryOwner}/${githubConfig.repositoryName}/releases"

        wmRuntimeInfo.wireMock.register(WireMock.get(WireMock.urlPathEqualTo(path))
            .withBasicAuth(githubConfig.repositoryOwner, githubConfig.accessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(HTTP_OK)
                // assume we already have one release with label 'v0.0.1'
                // on the github server
                .withBody("""[{"id": 1, "label": "v0.0.1"}]""")
            ))

        wmRuntimeInfo.wireMock.register(WireMock.post(WireMock.urlPathEqualTo(path))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.tag_name == '$releaseLabel')]"))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.name == '$releaseName')]"))
            .withBasicAuth(githubConfig.repositoryOwner, githubConfig.accessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(HTTP_CREATED)
                .withBody("""{"id": 1}""")
            ))

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(
              "-P${GithubConfig.PROPERTY_ACCESS_TOKEN}=${githubConfig.accessToken}",
              "createGithubRelease",
              "--release-label", releaseLabel,
              "--stacktrace"
            )
            .withPluginClasspath()
            .build()

        result.dumpOutputOnError("createGithubRelease")
        assertEquals(SUCCESS, result.task(":createGithubRelease")?.outcome)
    }

    /**
     * Can create a release
     * - for the current label in 'version'
     * - using the standard task 'createGithubRelease'
     */
    @Test
    fun test05(wmRuntimeInfo: WireMockRuntimeInfo) {

        val releaseName = "a test release"
        val releaseLabel = "v0.0.2"

        releaseFile.writeText("""
          releases:
            - label: "$releaseLabel"
              name: "$releaseName"
              minJosmVersion: 5678
              description: a test description
          """.trimIndent()
        )

        val githubConfig = project.buildGithubConfig(wmRuntimeInfo.httpBaseUrl, "github_user", "repo", "aaaabbbb")

        buildFile.writeText("""
            plugins {
                id("org.openstreetmap.josm")
            }
            version="$releaseLabel"
            josm {
                ${githubConfig.toGradleBuildscript()}
                josmCompileVersion = 14002
                manifest {
                    minJosmVersion = 14002
                    mainClass = "test_plugin.TestPlugin"
                }
            }
            """.trimIndent()
        )

        val path = "/repos/${githubConfig.repositoryOwner}/${githubConfig.repositoryName}/releases"

        wmRuntimeInfo.wireMock.register(WireMock.get(WireMock.urlPathEqualTo(path))
            .withBasicAuth(githubConfig.repositoryOwner, githubConfig.accessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(HTTP_OK)
                // assume we already have one release with label 'v0.0.1'
                // on the github server
                .withBody("""[{"id": 1, "label": "v0.0.1"}]""")
            ))

        wmRuntimeInfo.wireMock.register(WireMock.post(WireMock.urlPathEqualTo(path))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.tag_name == '$releaseLabel')]"))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.name == '$releaseName')]"))
            .withBasicAuth(githubConfig.repositoryOwner, githubConfig.accessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(HTTP_CREATED)
                .withBody("""{"id": 1}""")
            ))

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(
              "-P${GithubConfig.PROPERTY_ACCESS_TOKEN}=${githubConfig.accessToken}",
              "createGithubRelease",
              "--stacktrace"
            )
            .withPluginClasspath()
            .build()

        result.dumpOutputOnError("createGithubRelease")
        assertEquals(SUCCESS, result.task(":createGithubRelease")?.outcome)
    }

}



