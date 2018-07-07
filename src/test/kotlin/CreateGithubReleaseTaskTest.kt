package org.openstreetmap.josm.gradle.plugin

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.openstreetmap.josm.gradle.plugin.task.*
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver
import java.io.File

class CreateGithubReleaseTaskTest: BaseGithubReleaseTaskTest() {

    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `can create a release, arguments are configured in project properties`(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri apiUri: String) {

        val releaseName = "a test release"
        val releaseLabel = "v0.0.2"

        val releaseFileContent = """
          releases:
            - label: "$releaseLabel"
              name: "$releaseName"
              numeric_josm_version: 5678
              description: a test description
          """.trimIndent()
        prepareReleasesSpecs(releaseFileContent)

        val githubUser = "github_user"
        val githubAccessToken = "aaaabbbb"
        val githubRepo = "repo"

        val gradlePropertiesContent = """
            $CONFIG_OPT_GITHUB_USER = $githubUser
            $CONFIG_OPT_GITHUB_ACCESS_TOKEN = $githubAccessToken
            $CONFIG_OPT_GITHUB_REPOSITORY = $githubRepo
            $CONFIG_OPT_GITHUB_API_URL = $apiUri
            """.trimIndent()
        prepareGradleProperties(gradlePropertiesContent)

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
            }
            version="v0.0.1"
            josm {
                josmCompileVersion = 14002
                manifest {
                    minJosmVersion = 14002
                }
            }
            import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask
            task myCreateGithubRelease(type: CreateGithubReleaseTask){
              releaseLabel = "$releaseLabel"
            }
            """.trimIndent()
        prepareBuildFile(buildFileContent)

        val path = "/repos/$githubUser/$githubRepo/releases"

        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(path))
            .withBasicAuth(githubUser, githubAccessToken)
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
            .withBasicAuth(githubUser, githubAccessToken)
            .willReturn(WireMock.aResponse()
            .withStatus(200)
            .withBody("""{"id": 1}""")
        ))

        val result = GradleRunner.create()
          .withProjectDir(buildDir)
          .withArguments("myCreateGithubRelease")
          .build()
        assertEquals(SUCCESS, result.task(":myCreateGithubRelease")?.outcome)
        println(result.output)
    }

    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `can derive a default github repo name`(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri apiUri: String) {

        val releaseLabel = "v0.0.2"

        val releaseFileContent = """
          releases:
            - label: "$releaseLabel"
              numeric_josm_version: 5678
          """.trimIndent()
        prepareReleasesSpecs(releaseFileContent)

        val githubUser = "github_user"
        val githubAccessToken = "aaaabbbb"

        // the name of the build dir is the default github repo name
        val githubRepo = buildDir!!.name

        // don't configure the github repo, should be derived from the project
        // name
        val gradlePropertiesContent = """
            $CONFIG_OPT_GITHUB_USER = $githubUser
            $CONFIG_OPT_GITHUB_ACCESS_TOKEN = $githubAccessToken
            $CONFIG_OPT_GITHUB_API_URL = $apiUri
            """.trimIndent()
        prepareGradleProperties(gradlePropertiesContent)

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
            }

            version="v0.0.1"
            josm {
                josmCompileVersion = 14002
                manifest {
                    minJosmVersion = 14002
                }
            }

            import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask

            task myCreateGithubRelease(type: CreateGithubReleaseTask){
              releaseLabel = "$releaseLabel"
            }
            """.trimIndent()
        prepareBuildFile(buildFileContent)

        val path = "/repos/$githubUser/$githubRepo/releases"

        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(path))
            .withBasicAuth(githubUser, githubAccessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                // assume we only have one release with label 'v0.0.1'
                .withBody("""[{"id": 1, "label": "v0.0.1"}]""")
            ))

        server.stubFor(WireMock.post(WireMock.urlPathEqualTo(path))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.tag_name == '$releaseLabel')]"))
            .withBasicAuth(githubUser, githubAccessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withBody("""{"id": 1}""")
            ))


        val result = GradleRunner.create()
            .withProjectDir(buildDir)
            .withArguments("myCreateGithubRelease")
            .build()
        assertEquals(SUCCESS, result.task(":myCreateGithubRelease")?.outcome)
        println(result.output)
    }

    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `should create the default latest release`(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri apiUri: String) {

        val releaseLabel = DEFAULT_LATEST_LABEL

        val releaseFileContent = """
          releases:
          """.trimIndent()
        prepareReleasesSpecs(releaseFileContent)

        val githubUser = "github_user"
        val githubAccessToken = "aaaabbbb"

        // the name of the build dir is the default github repo name
        val githubRepo = buildDir!!.name

        // don't configure the github repo, should be derived from the project
        // name
        val gradlePropertiesContent = """
            $CONFIG_OPT_GITHUB_USER = $githubUser
            $CONFIG_OPT_GITHUB_ACCESS_TOKEN = $githubAccessToken
            $CONFIG_OPT_GITHUB_API_URL = $apiUri
            """.trimIndent()
        prepareGradleProperties(gradlePropertiesContent)

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
            }

            version="v0.0.1"
            josm {
                josmCompileVersion = 14002
                manifest {
                    minJosmVersion = 14002
                }
            }

            import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask

            task myCreateGithubRelease(type: CreateGithubReleaseTask){
              releaseLabel = "$releaseLabel"
            }
            """.trimIndent()
        prepareBuildFile(buildFileContent)

        val path = "/repos/$githubUser/$githubRepo/releases"

        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(path))
            .withBasicAuth(githubUser, githubAccessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                // assume we only have one release with label 'v0.0.1'
                .withBody("""[{"id": 1, "label": "v0.0.1"}]""")
            ))

        server.stubFor(WireMock.post(WireMock.urlPathEqualTo(path))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.tag_name == '$releaseLabel')]"))
            .withBasicAuth(githubUser, githubAccessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withBody("""{"id": 2}""")
            ))


        val result = GradleRunner.create()
            .withProjectDir(buildDir)
            .withArguments("myCreateGithubRelease")
            .build()
        assertEquals(SUCCESS, result.task(":myCreateGithubRelease")?.outcome)
        println(result.output)
    }

    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `should create a custom latest release`(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri apiUri: String) {

        val releaseLabel = "current"

        val releaseFileContent = """
          latest_release:
            label: $releaseLabel

          releases:
          """.trimIndent()
        prepareReleasesSpecs(releaseFileContent)

        val githubUser = "github_user"
        val githubAccessToken = "aaaabbbb"

        // the name of the build dir is the default github repo name
        val githubRepo = buildDir!!.name

        // don't configure the github repo, should be derived from the project
        // name
        val gradlePropertiesContent = """
            $CONFIG_OPT_GITHUB_USER = $githubUser
            $CONFIG_OPT_GITHUB_ACCESS_TOKEN = $githubAccessToken
            $CONFIG_OPT_GITHUB_API_URL = $apiUri
            """.trimIndent()
        prepareGradleProperties(gradlePropertiesContent)

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
            }

            version="v0.0.1"
            josm {
                josmCompileVersion = 14002
                manifest {
                    minJosmVersion = 14002
                }
            }

            import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask

            task myCreateGithubRelease(type: CreateGithubReleaseTask){
              releaseLabel = "$releaseLabel"
            }
            """.trimIndent()
        prepareBuildFile(buildFileContent)

        val path = "/repos/$githubUser/$githubRepo/releases"

        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(path))
            .withBasicAuth(githubUser, githubAccessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                // assume we only have one release with label 'v0.0.1'
                .withBody("""[{"id": 1, "label": "v0.0.1"}]""")
            ))

        server.stubFor(WireMock.post(WireMock.urlPathEqualTo(path))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.tag_name == '$releaseLabel')]"))
            .withBasicAuth(githubUser, githubAccessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withBody("""{"id": 2}""")
            ))


        val result = GradleRunner.create()
            .withProjectDir(buildDir)
            .withArguments("myCreateGithubRelease")
            .build()
        assertEquals(SUCCESS, result.task(":myCreateGithubRelease")?.outcome)
        println(result.output)
    }
}



