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
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_OK

class CreatePickupReleaseTaskTest: BaseGithubReleaseTaskTest() {

    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `can create a default pickup release`(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri apiUri: String) {

        val releaseName = DEFAULT_PICKUP_RELEASE_NAME
        val releaseLabel = DEFAULT_PICKUP_RELEASE_LABEL

        val releaseFileContent = """
          releases:
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
                    mainClass = "test_plugin.TestPlugin"
                }
            }
            """.trimIndent()
        prepareBuildFile(buildFileContent)

        val path = "/repos/$githubUser/$githubRepo/releases"

        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(path))
            .withBasicAuth(githubUser, githubAccessToken)
            .willReturn(WireMock.aResponse()
            .withStatus(HTTP_OK)
            // assume we have no releases yet
            .withBody("""[]""")
        ))

        server.stubFor(WireMock.post(WireMock.urlPathEqualTo(path))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.tag_name == '$releaseLabel')]"))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.name == '$releaseName')]"))
            .withBasicAuth(githubUser, githubAccessToken)
            .willReturn(WireMock.aResponse()
            .withStatus(HTTP_CREATED)
            .withBody("""{"id": 1}""")
        ))

        val result = GradleRunner.create()
          .withProjectDir(buildDir)
          .withArguments("createPickupRelease",
              "--stacktrace"
          )
          .build()
        result.dumpOutputOnError("createPickupRelease")
        assertEquals(SUCCESS, result.task(":createPickupRelease")?.outcome)
    }


    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `can create a custom pickup release`(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri apiUri: String) {

        val releaseLabel = "my-pickup-release"
        val releaseName = "My Pickup Release"

        val releaseFileContent = """
          pickup_release_for_josm:
            label: $releaseLabel
            name: $releaseName
            description:

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
                    mainClass = "test_plugin.TestPlugin"
                }
            }
            """.trimIndent()
        prepareBuildFile(buildFileContent)

        val path = "/repos/$githubUser/$githubRepo/releases"

        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(path))
            .withBasicAuth(githubUser, githubAccessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(HTTP_OK)
                // assume we only have one release with label 'v0.0.1'
                .withBody("""[]""")
            ))

        server.stubFor(WireMock.post(WireMock.urlPathEqualTo(path))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.tag_name == '$releaseLabel')]"))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.name == '$releaseName')]"))
            .withBasicAuth(githubUser, githubAccessToken)
            .willReturn(WireMock.aResponse()
                .withStatus(HTTP_CREATED)
                .withBody("""{"id": 2}""")
            ))

        val result = GradleRunner.create()
            .withProjectDir(buildDir)
            .withArguments("createPickupRelease",
                "--stacktrace")
            .build()
        result.dumpOutputOnError("createPickupRelease")
        assertEquals(SUCCESS, result.task(":createPickupRelease")?.outcome)
    }
}



