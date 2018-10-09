package org.openstreetmap.josm.gradle.plugin

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.openstreetmap.josm.gradle.plugin.config.GithubConfig
import org.openstreetmap.josm.gradle.plugin.github.DEFAULT_PICKUP_RELEASE_NAME
import org.openstreetmap.josm.gradle.plugin.github.DEFAULT_PICKUP_RELEASE_LABEL
import org.openstreetmap.josm.gradle.plugin.testutils.toGradleBuildscript
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

        val githubConfig = GithubConfig(ProjectBuilder.builder().build()).apply {
          repositoryOwner = "github_user"
          repositoryName = "repo"
          accessToken = "aaaabbbb"
          apiUrl = apiUri
        }

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
            }
            version="v0.0.1"
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
            // assume we have no releases yet
            .withBody("""[]""")
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


        val githubConfig = GithubConfig(ProjectBuilder.builder().build()).apply {
          repositoryOwner = "github_user"
          repositoryName = "repo_name"
          accessToken = "aaaabbbb"
          apiUrl = apiUri
        }

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
            }

            version="v0.0.1"
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
                // assume we only have one release with label 'v0.0.1'
                .withBody("""[]""")
            ))

        server.stubFor(WireMock.post(WireMock.urlPathEqualTo(path))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.tag_name == '$releaseLabel')]"))
            .withRequestBody(WireMock.matchingJsonPath(
                "$[?(@.name == '$releaseName')]"))
            .withBasicAuth(githubConfig.repositoryOwner, githubConfig.accessToken)
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



