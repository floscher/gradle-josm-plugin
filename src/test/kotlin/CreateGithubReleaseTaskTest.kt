package org.openstreetmap.josm.gradle.plugin

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.openstreetmap.josm.gradle.plugin.task.CONFIG_OPT_GITHUB_ACCESS_TOKEN
import org.openstreetmap.josm.gradle.plugin.task.CONFIG_OPT_GITHUB_API_URL
import org.openstreetmap.josm.gradle.plugin.task.CONFIG_OPT_GITHUB_REPOSITORY
import org.openstreetmap.josm.gradle.plugin.task.CONFIG_OPT_GITHUB_USER
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver
import java.io.File

class CreateGithubReleaseTaskTest: BaseGithubReleaseTaskTest() {

    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `can create a release, when arguments are configured in the task`(
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
        File(buildDir, "releases.yml").printWriter().use {
            it.println(releaseFileContent)
        }

        val githubUser = "github_user"
        val githubAccessToken = "aaaabbbb"
        val githubRepo = "repo"

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
            }

            import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask

            task myCreateGithubRelease(type: CreateGithubReleaseTask){
              releaseLabel = "$releaseLabel"
              githubUser = "$githubUser"
              githubAccessToken = "$githubAccessToken"
              githubRepository = "$githubRepo"
              githubApiUrl = "$apiUri"
            }
            """.trimIndent()
        buildFile?.printWriter()?.use {it.println(buildFileContent)}

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
    fun `can create a release, when arguments are configured on the command line`(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri apiUri: String) {

        val releaseName = "a test release"
        val releaseLabel = "v0.0.2"

        val relaeaseFileContent = """
          releases:
            - label: "$releaseLabel"
              name: "$releaseName"
              numeric_josm_version: 5678
              description: a test description
          """.trimIndent()
        File(buildDir, "releases.yml").printWriter().use {
            it.println(relaeaseFileContent)
        }

        val githubUser = "github_user"
        val githubAccessToken = "aaaabbbb"
        val githubRepo = "repo"

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
            }

            import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask

            task myCreateGithubRelease(type: CreateGithubReleaseTask){
            }
            """.trimIndent()
        buildFile?.printWriter()?.use {it.println(buildFileContent)}

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
            .withRequestBody(WireMock.matchingJsonPath(
              "$[?(@.name == '$releaseName')]"))
            .withBasicAuth(githubUser, githubAccessToken)
            .willReturn(WireMock.aResponse()
            .withStatus(200)
            .withBody("""{"id": 1}""")
        ))

        val result = GradleRunner.create()
            .withProjectDir(buildDir)
            .withArguments("myCreateGithubRelease",
                "--release-label", releaseLabel,
                "--github-api-url", apiUri,
                "--github-repository", githubRepo,
                "--github-user", githubUser,
                "--github-access-token", githubAccessToken
            )
            .build()
        assertEquals(SUCCESS, result.task(":myCreateGithubRelease")?.outcome)
        println(result.output)
    }

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
        File(buildDir, "releases.yml").printWriter().use {
            it.println(releaseFileContent)
        }

        val githubUser = "github_user"
        val githubAccessToken = "aaaabbbb"
        val githubRepo = "repo"

        val gradlePropertiesContent = """
            $CONFIG_OPT_GITHUB_USER = $githubUser
            $CONFIG_OPT_GITHUB_ACCESS_TOKEN = $githubAccessToken
            $CONFIG_OPT_GITHUB_REPOSITORY = $githubRepo
            $CONFIG_OPT_GITHUB_API_URL = $apiUri
            """.trimIndent()
        File(buildDir, "gradle.properties").printWriter().use {
            it.println(gradlePropertiesContent)
        }

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
            }

            import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask

            task myCreateGithubRelease(type: CreateGithubReleaseTask){
              releaseLabel = "$releaseLabel"
            }
            """.trimIndent()
        buildFile?.printWriter()?.use {it.println(buildFileContent)}

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
    fun `can create a release using pre-configured task and cmd line args`(
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
        File(buildDir, "releases.yml").printWriter().use {
            it.println(releaseFileContent)
        }

        val githubUser = "github_user"
        val githubAccessToken = "aaaabbbb"
        val githubRepo = "repo"

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
            }

            // The task 'createGithubRelease' is predefined
            """.trimIndent()
        buildFile?.printWriter()?.use {it.println(buildFileContent)}

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
            .withArguments("createGithubRelease",
                "--release-label", releaseLabel,
                "--github-api-url", apiUri,
                "--github-repository", githubRepo,
                "--github-user", githubUser,
                "--github-access-token", githubAccessToken
            )
            .build()
        assertEquals(SUCCESS, result.task(":createGithubRelease")?.outcome)
        println(result.output)
    }

    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `can create a release using pre-configured task with parameters`(
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
        File(buildDir, "releases.yml").printWriter().use {
            it.println(releaseFileContent)
        }

        val githubUser = "github_user"
        val githubAccessToken = "aaaabbbb"
        val githubRepo = "repo"

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
            }
            createGithubRelease {
              releaseLabel = "$releaseLabel"
              githubUser = "$githubUser"
              githubAccessToken = "$githubAccessToken"
              githubRepository = "$githubRepo"
              githubApiUrl = "$apiUri"
            }
            """.trimIndent()
        buildFile?.printWriter()?.use {it.println(buildFileContent)}

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
            .withArguments("createGithubRelease")
            .build()
        assertEquals(SUCCESS, result.task(":createGithubRelease")?.outcome)
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
        File(buildDir, "releases.yml").printWriter().use {
            it.println(releaseFileContent)
        }

        val githubUser = "github_user"
        val githubAccessToken = "aaaabbbb"

        // the name of the build dir is the default github repo name
        val githubRepo = buildDir!!.name

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
            }

            import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask

            task myCreateGithubRelease(type: CreateGithubReleaseTask){
              releaseLabel = "$releaseLabel"
              githubUser = "$githubUser"
              githubAccessToken = "$githubAccessToken"
              // don't configure the repo. In this test we want to
              // use the default value
              // githubRepository = "$githubRepo"
              githubApiUrl = "$apiUri"
            }
            """.trimIndent()
        buildFile?.printWriter()?.use {it.println(buildFileContent)}

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
}



