package org.openstreetmap.josm.gradle.plugin.task.github

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.openstreetmap.josm.gradle.plugin.config.PROPERTY_ACCESS_TOKEN
import org.openstreetmap.josm.gradle.plugin.testutils.buildGithubConfig
import org.openstreetmap.josm.gradle.plugin.testutils.toGradleBuildscript
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_OK

class CreateGithubReleaseTaskTest: BaseGithubReleaseTaskTest() {

    /**
     * Can create a release
     * - for a custom release label
     * - configured in a custom task in the build file
     */
    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun test01(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri apiUri: String) {

        val releaseName = "a test release"
        val releaseLabel = "v0.0.2"

        val releaseFileContent = """
          releases:
            - label: "$releaseLabel"
              name: "$releaseName"
              minJosmVersion: 5678
              description: a test description
          """.trimIndent()
        prepareReleasesSpecs(releaseFileContent)

        val githubConfig = buildGithubConfig(apiUri, "github_user", "repo", "aaaabbbb")

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
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
        prepareBuildFile(buildFileContent)

        val path = "/repos/${githubConfig.repositoryOwner}/${githubConfig.repositoryName}/releases"

        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(path))
            .withBasicAuth(githubConfig.repositoryOwner, githubConfig.accessToken)
            .willReturn(WireMock.aResponse()
            .withStatus(200)
            // assume we already have one release with label 'v0.0.1'
            // on the github server
            .withBody("""[{"id": 1, "label": "v0.0.1"}]""")
        ))

        server.stubFor(WireMock.post(WireMock.urlPathEqualTo(path))
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
          .withProjectDir(buildDir)
          .withArguments(
            "-P$PROPERTY_ACCESS_TOKEN=${githubConfig.accessToken}",
            "myCreateGithubRelease",
            "--stacktrace"
          )
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
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun test02(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri apiUri: String) {

        val releaseName = "a test release"
        val releaseLabel = "v0.0.2"

        val releaseFileContent = """
          releases:
            - label: "$releaseLabel"
              name: "$releaseName"
              minJosmVersion: 5678
              description: a test description
          """.trimIndent()
        prepareReleasesSpecs(releaseFileContent)

        val githubConfig = buildGithubConfig(apiUri, "JOSM", "some-repo", "abcdefghijklmnopqrstuvwxyz")

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
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
        prepareBuildFile(buildFileContent)

        val path = "/repos/${githubConfig.repositoryOwner}/${githubConfig.repositoryName}/releases"

        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(path))
            .withBasicAuth(githubConfig.repositoryOwner, githubConfig.accessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(HTTP_OK)
                // assume we already have one release with label 'v0.0.1'
                // on the github server
                .withBody("""[{"id": 1, "label": "v0.0.1"}]""")
            ))

        server.stubFor(WireMock.post(WireMock.urlPathEqualTo(path))
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
            .withProjectDir(buildDir)
            .withArguments(
              "-P$PROPERTY_ACCESS_TOKEN=${githubConfig.accessToken}",
              "createGithubRelease",
              "--stacktrace"
            )
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
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun test03(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri apiUri: String) {

        val releaseLabel = "v0.0.2"

        val releaseFileContent = """
          releases:
            - label: "$releaseLabel"
              minJosmVersion: 5678
          """.trimIndent()
        prepareReleasesSpecs(releaseFileContent)

        val githubConfig = buildGithubConfig(apiUri, "github_user", "repoName", "aaaabbbb")

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
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
        prepareBuildFile(buildFileContent)

        val path = "/repos/${githubConfig.repositoryOwner}/${githubConfig.repositoryName}/releases"

        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(path))
            .withBasicAuth(githubConfig.repositoryOwner, githubConfig.accessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(HTTP_OK)
                // assume we only have one release with label 'v0.0.1'
                .withBody("""[{"id": 1, "label": "v0.0.1"}]""")
            ))

        server.stubFor(WireMock.post(WireMock.urlPathEqualTo(path))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.tag_name == '$releaseLabel')]"))
            .withBasicAuth(githubConfig.repositoryOwner, githubConfig.accessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(HTTP_CREATED)
                .withBody("""{"id": 1}""")
            ))


        val result = GradleRunner.create()
            .withProjectDir(buildDir)
            .withArguments(
              "-P$PROPERTY_ACCESS_TOKEN=${githubConfig.accessToken}",
              "createGithubRelease",
              "--stacktrace"
            )
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
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun test04(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri apiUri: String) {

        val releaseName = "a test release"
        val releaseLabel = "v0.0.2"

        val releaseFileContent = """
          releases:
            - label: "$releaseLabel"
              name: "$releaseName"
              minJosmVersion: 5678
              description: a test description
          """.trimIndent()
        prepareReleasesSpecs(releaseFileContent)

        val githubConfig = buildGithubConfig(apiUri, "github_user", "repo", "aaaabbbb")

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
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
        prepareBuildFile(buildFileContent)

        val path = "/repos/${githubConfig.repositoryOwner}/${githubConfig.repositoryName}/releases"

        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(path))
            .withBasicAuth(githubConfig.repositoryOwner, githubConfig.accessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(HTTP_OK)
                // assume we already have one release with label 'v0.0.1'
                // on the github server
                .withBody("""[{"id": 1, "label": "v0.0.1"}]""")
            ))

        server.stubFor(WireMock.post(WireMock.urlPathEqualTo(path))
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
            .withProjectDir(buildDir)
            .withArguments(
              "-P$PROPERTY_ACCESS_TOKEN=${githubConfig.accessToken}",
              "createGithubRelease",
              "--release-label", releaseLabel,
              "--stacktrace"
            )
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
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun test05(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri apiUri: String) {

        val releaseName = "a test release"
        val releaseLabel = "v0.0.2"

        val releaseFileContent = """
          releases:
            - label: "$releaseLabel"
              name: "$releaseName"
              minJosmVersion: 5678
              description: a test description
          """.trimIndent()
        prepareReleasesSpecs(releaseFileContent)

        val githubConfig = buildGithubConfig(apiUri, "github_user", "repo", "aaaabbbb")

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
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
        prepareBuildFile(buildFileContent)

        val path = "/repos/${githubConfig.repositoryOwner}/${githubConfig.repositoryName}/releases"

        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(path))
            .withBasicAuth(githubConfig.repositoryOwner, githubConfig.accessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(HTTP_OK)
                // assume we already have one release with label 'v0.0.1'
                // on the github server
                .withBody("""[{"id": 1, "label": "v0.0.1"}]""")
            ))

        server.stubFor(WireMock.post(WireMock.urlPathEqualTo(path))
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
            .withProjectDir(buildDir)
            .withArguments(
              "-P$PROPERTY_ACCESS_TOKEN=${githubConfig.accessToken}",
              "createGithubRelease",
              "--stacktrace"
            )
            .build()

        result.dumpOutputOnError("createGithubRelease")
        assertEquals(SUCCESS, result.task(":createGithubRelease")?.outcome)
    }

}



