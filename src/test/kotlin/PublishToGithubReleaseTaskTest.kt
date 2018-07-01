package org.openstreetmap.josm.gradle.plugin

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.openstreetmap.josm.gradle.plugin.task.*
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver

class PublishToGithubReleaseTaskTest : BaseGithubReleaseTaskTest() {

    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `configured with task parameters, with custom remote jar name`(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri uri: String
    ) {

        val minJosmVersion = 1111
        val releaseId = 12345678
        val releaseLabel = "v0.0.1"
        val githubRepo = "repo_xy"

        val gradlePropertiesContent = """
            $CONFIG_OPT_GITHUB_REPOSITORY = $githubRepo
            $CONFIG_OPT_GITHUB_API_URL = $uri
            $CONFIG_OPT_GITHUB_UPLOAD_URL = $uri
            """.trimIndent()
        prepareGradleProperties(gradlePropertiesContent)

        val localJarName = "test-$releaseLabel.jar"
        val remoteJarName = "test.jar"

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
                id 'java'
            }
            version = "$releaseLabel"
            jar.archiveName = "$localJarName"
            josm {
              josmCompileVersion = "latest"
              manifest {
                  description = 'test'
                  minJosmVersion = $minJosmVersion
                  mainClass = 'test_plugin.TestPlugin'
              }
            }

            import org.openstreetmap.josm.gradle.plugin.task.PublishToGithubReleaseTask
            task myPublishToGithubRelease(type: PublishToGithubReleaseTask){
                releaseLabel = "$releaseLabel"
                remoteJarName = "$remoteJarName"
            }
            """

        val releasesContent = """
              releases:
                - label: $releaseLabel
                  numeric_josm_version: $minJosmVersion
            """.trimIndent()


        fun prepareAPIStub() {
            val githubUser = System.getenv(ENV_VAR_GITHUB_USER)
                ?: throw Exception(
                "env variable $ENV_VAR_GITHUB_USER not set"
            )
            // stub for "get releases"
            val path1 = "/repos/$githubUser/$githubRepo/releases"
            server.stubFor(get(WireMock.urlPathMatching("$path1.*"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(
                        """[{"id": $releaseId, "tag_name": "$releaseLabel"}]""")
                )
            )

            // stub for "upload release asset"
            val path2 = "/repos/$githubUser/$githubRepo/releases/$releaseId/assets"
            server.stubFor(post(WireMock.urlPathMatching("$path2.*"))
                .withHeader("Content-Type", equalTo(MEDIA_TYPE_JAR))
                .withQueryParam("name", equalTo(remoteJarName))
                //.withQueryParam("label", equalTo("test_plugin.jar"))
                .willReturn(aResponse()
                    .withStatus(201)
                    .withBody("""{"id": 1}""")
                )
            )
        }

        prepareBuildFile(buildFileContent)
        prepareTestPluginSource()
        prepareReleasesSpecs(releasesContent)
        prepareAPIStub()

        val result = GradleRunner.create()
            .withProjectDir(buildDir)
            .withArguments("build", "myPublishToGithubRelease")
            .build()
        Assertions.assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":myPublishToGithubRelease")?.outcome
        )
    }


    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `should publish to latest release too`(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri uri: String
    ) {

        val minJosmVersion = 1111
        val releaseId = 12345678
        val releaseLabel = "v0.0.1"
        val githubRepo = "repo_xy"

        val gradlePropertiesContent = """
            $CONFIG_OPT_GITHUB_REPOSITORY = $githubRepo
            $CONFIG_OPT_GITHUB_API_URL = $uri
            $CONFIG_OPT_GITHUB_UPLOAD_URL = $uri
            """.trimIndent()
        prepareGradleProperties(gradlePropertiesContent)

        val latestReleaseLabel = "most_recent"
        val latestReleaseId = "45681234"

        val localJarName = "test-$releaseLabel.jar"
        val remoteJarName = "test.jar"

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
                id 'java'
            }
            version = "$releaseLabel"
            jar.archiveName = "$localJarName"
            josm {
              josmCompileVersion = "latest"
              manifest {
                  description = 'test'
                  minJosmVersion = $minJosmVersion
                  mainClass = 'test_plugin.TestPlugin'
              }
            }

            import org.openstreetmap.josm.gradle.plugin.task.PublishToGithubReleaseTask
            task myPublishToGithubRelease(type: PublishToGithubReleaseTask){
                releaseLabel = "$releaseLabel"
                remoteJarName = "$remoteJarName"
                // upload to latest release to
                updateLatest = true
            }
            """

        val releasesContent = """
              latest_release:
                name: $latestReleaseLabel
              releases:
                - label: $releaseLabel
                  numeric_josm_version: $minJosmVersion
            """.trimIndent()


        fun prepareAPIStub() {
            val githubUser = System.getenv(ENV_VAR_GITHUB_USER)
                ?: throw Exception(
                "env variable $ENV_VAR_GITHUB_USER not set"
            )
            // stub for "get releases"
            val path1 = "/repos/$githubUser/$githubRepo/releases"
            server.stubFor(get(WireMock.urlPathMatching("$path1.*"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("""[
                    |   {"id": $releaseId, "tag_name": "$releaseLabel"},
                    |   {"id": $latestReleaseId,
                    |    "tag_name": "$latestReleaseLabel"
                    |   }
                    |]""".trimMargin())
                )
            )

            // stub for "upload release asset"
            val path2 = "/repos/$githubUser/$githubRepo/releases/" +
                releaseId + "/assets"
            server.stubFor(post(WireMock.urlPathMatching("$path2.*"))
                .withHeader("Content-Type", equalTo(MEDIA_TYPE_JAR))
                .withQueryParam("name", equalTo(remoteJarName))
                //.withQueryParam("label", equalTo("test_plugin.jar"))
                .willReturn(aResponse()
                    .withStatus(201)
                    .withBody("""{"id": 1}""")
                )
            )

            // stub for "upload release asset" to the latest release
            val path3 = "/repos/$githubUser/$githubRepo/releases/" +
                latestReleaseId + "/assets"
            server.stubFor(post(WireMock.urlPathMatching("$path3.*"))
                .withHeader("Content-Type", equalTo(MEDIA_TYPE_JAR))
                .withQueryParam("name", equalTo(remoteJarName))
                //.withQueryParam("label", equalTo("test_plugin.jar"))
                .willReturn(aResponse()
                    .withStatus(201)
                    .withBody("""{"id": 2}""")
                )
            )

        }

        prepareBuildFile(buildFileContent)
        prepareTestPluginSource()
        prepareReleasesSpecs(releasesContent)
        prepareAPIStub()

        val result = GradleRunner.create()
            .withProjectDir(buildDir)
            .withArguments("build", "myPublishToGithubRelease")
            .build()
        Assertions.assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":myPublishToGithubRelease")?.outcome
        )
        println(result.output)
    }

    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `minimal config, on command line, use standard task`(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri uri: String) {

        val minJosmVersion = 1111
        val releaseId = 12345678
        val releaseLabel = "v0.0.1"
        val githubRepo = "repo_xy"

        val gradlePropertiesContent = """
            $CONFIG_OPT_GITHUB_REPOSITORY = $githubRepo
            $CONFIG_OPT_GITHUB_API_URL = $uri
            $CONFIG_OPT_GITHUB_UPLOAD_URL = $uri
            """.trimIndent()
        prepareGradleProperties(gradlePropertiesContent)

        // the standard pattern for the jar name the gradle-josm-plugin sets
        // for the plugin jar
        val localJarName = "${buildDir?.name}-$releaseLabel.jar"

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
                id 'java'
            }
            version = "$releaseLabel"
            josm {
              josmCompileVersion = "latest"
              manifest {
                  description = 'test'
                  minJosmVersion = $minJosmVersion
                  mainClass = 'test_plugin.TestPlugin'
              }
            }
            // will invoke the standard task 'publishToGithubRelease'
            """

        val releasesContent = """
              releases:
                - label: $releaseLabel
                  numeric_josm_version: $minJosmVersion
            """.trimIndent()



        fun prepareAPIStub() {
            val githubUser = System.getenv(ENV_VAR_GITHUB_USER)
                ?: throw Exception(
                    "env variable $ENV_VAR_GITHUB_USER not set"
                )
            // stub for "get releases"
            val path1 = "/repos/$githubUser/$githubRepo/releases"
            server.stubFor(get(WireMock.urlPathMatching("$path1.*"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(
                        """[{"id": $releaseId, "tag_name": "$releaseLabel"}]""")
                )
            )

            // stub for "upload release asset"
            val path2 = "/repos/$githubUser/$githubRepo/releases/$releaseId/assets"
            server.stubFor(post(WireMock.urlPathMatching("$path2.*"))
                .withHeader("Content-Type", equalTo(MEDIA_TYPE_JAR))
                .withQueryParam("name", equalTo(localJarName))
                //.withQueryParam("label", equalTo("test_plugin.jar"))
                .willReturn(aResponse()
                    .withStatus(201)
                    .withBody("""{"id": 1}""")
                )
            )
        }

        prepareTestPluginSource()
        prepareBuildFile(buildFileContent)
        prepareReleasesSpecs(releasesContent)
        prepareAPIStub()

        val result = GradleRunner.create()
            .withProjectDir(buildDir)
            .withArguments("build", "publishToGithubRelease",
                "--release-label", releaseLabel
            )
            .build()
        Assertions.assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":publishToGithubRelease")?.outcome
        )
    }


    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `use standard task, full config in the task`(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri uri: String
    ) {

        val minJosmVersion = 1111
        val releaseId = 12345678
        val releaseLabel = "v0.0.1"
        val githubRepo = "repo_xy"

        val gradlePropertiesContent = """
            $CONFIG_OPT_GITHUB_REPOSITORY = $githubRepo
            $CONFIG_OPT_GITHUB_API_URL = $uri
            $CONFIG_OPT_GITHUB_UPLOAD_URL = $uri
            """.trimIndent()
        prepareGradleProperties(gradlePropertiesContent)

        val latestReleaseLabel = "most_recent"
        val latestReleaseId = "45681234"

        val localJarName = "test-$releaseLabel.jar"
        val remoteJarName = "test.jar"

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
                id 'java'
            }
            version = "$releaseLabel"
            jar.archiveName = "$localJarName"
            josm {
              josmCompileVersion = "latest"
              manifest {
                  description = 'test'
                  minJosmVersion = $minJosmVersion
                  mainClass = 'test_plugin.TestPlugin'
              }
            }

            publishToGithubRelease {
                // githubUser from env var GITHUB_USER
                // githubAccessToken from env var GITHUB_ACCESS_TOKEN
                releaseLabel = "$releaseLabel"
                remoteJarName = "$remoteJarName"
                // upload to latest release to
                updateLatest = true
            }
            """

        val releasesContent = """
              latest_release:
                name: $latestReleaseLabel
              releases:
                - label: $releaseLabel
                  numeric_josm_version: $minJosmVersion
            """.trimIndent()


        fun prepareAPIStub() {
            val githubUser = System.getenv(ENV_VAR_GITHUB_USER)
                ?: throw Exception(
                    "env variable $ENV_VAR_GITHUB_USER not set"
                )
            // stub for "get releases"
            val path1 = "/repos/$githubUser/$githubRepo/releases"
            server.stubFor(get(WireMock.urlPathMatching("$path1.*"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("""[
                    |   {"id": $releaseId, "tag_name": "$releaseLabel"},
                    |   {"id": $latestReleaseId,
                    |    "tag_name": "$latestReleaseLabel"
                    |   }
                    |]""".trimMargin())
                )
            )

            // stub for "upload release asset"
            val path2 = "/repos/$githubUser/$githubRepo/releases/" +
                releaseId + "/assets"
            server.stubFor(post(WireMock.urlPathMatching("$path2.*"))
                .withHeader("Content-Type", equalTo(MEDIA_TYPE_JAR))
                .withQueryParam("name", equalTo(remoteJarName))
                //.withQueryParam("label", equalTo("test_plugin.jar"))
                .willReturn(aResponse()
                    .withStatus(201)
                    .withBody("""{"id": 1}""")
                )
            )

            // stub for "upload release asset" to the latest release
            val path3 = "/repos/$githubUser/$githubRepo/releases/" +
                latestReleaseId + "/assets"
            server.stubFor(post(WireMock.urlPathMatching("$path3.*"))
                .withHeader("Content-Type", equalTo(MEDIA_TYPE_JAR))
                .withQueryParam("name", equalTo(remoteJarName))
                //.withQueryParam("label", equalTo("test_plugin.jar"))
                .willReturn(aResponse()
                    .withStatus(201)
                    .withBody("""{"id": 2}""")
                )
            )
        }

        prepareBuildFile(buildFileContent)
        prepareTestPluginSource()
        prepareReleasesSpecs(releasesContent)
        prepareAPIStub()

        val result = GradleRunner.create()
            .withProjectDir(buildDir)
            .withArguments("build", "publishToGithubRelease")
            .build()
        Assertions.assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":publishToGithubRelease")?.outcome
        )
        println(result.output)
    }
}
