package org.openstreetmap.josm.gradle.plugin

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.openstreetmap.josm.gradle.plugin.config.GithubConfig
import org.openstreetmap.josm.gradle.plugin.task.github.MEDIA_TYPE_JAR
import org.openstreetmap.josm.gradle.plugin.task.github.PublishToGithubReleaseTask
import org.openstreetmap.josm.gradle.plugin.testutils.toGradleBuildscript
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver

class PublishToGithubReleaseTaskTest : BaseGithubReleaseTaskTest() {

    fun BuildResult.assertMessageInOutput(message: String) {
        val pattern = Regex(message, setOf(RegexOption.MULTILINE))
        assertTrue(pattern.containsMatchIn(this.output))
    }

    fun githubConfig(uri: String) = GithubConfig(ProjectBuilder.builder().build()).apply {
      repositoryName = "repo_xy"
      apiUrl = uri
      uploadUrl = uri
      repositoryOwner = GITHUB_USER
      accessToken = "asdfalkasdhf"
    }

    /**
     * runs a build with custom PublishToGithubReleaseTask
     *  - with parameters supplied as task configuration
     *  - for a custom remote jar name
     */
    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun test_01(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri uri: String
    ) {

        val minJosmVersion = 1111
        val releaseId = 12345678
        val releaseLabel = "v0.0.1"

        val githubConfig = githubConfig(uri)

        val localJarName = "test-$releaseLabel.jar"
        val remoteJarName = "test.jar"

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
                id 'java'
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
            """

        val releasesContent = """
              releases:
                - label: $releaseLabel
                  numeric_josm_version: $minJosmVersion
            """.trimIndent()
        prepareReleasesSpecs(releasesContent)

        fun prepareAPIStub() {
            // stub for "get releases"
            val path1 = "/repos/$GITHUB_USER/${githubConfig.repositoryName}/releases"
            server.stubFor(get(urlPathEqualTo(path1))
                .inScenario("upload-non-existing-asset")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(
                        """[{"id": $releaseId,
                        "tag_name": "$releaseLabel"}]"""
                    )
                )
            )

            // stub get release assets
            val path2 = "/repos/$GITHUB_USER/${githubConfig.repositoryName}/releases/$releaseId/assets"
            server.stubFor(get(urlPathEqualTo(path2))
                .inScenario("upload-non-existing-asset")
                .willReturn(aResponse()
                    .withStatus(200)
                    // no assets
                    .withBody("[]")
                )
            )

            // stub for "upload release asset"
            val path3 = "/repos/$GITHUB_USER/${githubConfig.repositoryName}/releases/$releaseId/assets"
            server.stubFor(post(urlPathEqualTo(path3))
                .inScenario("upload-non-existing-asset")
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
        prepareAPIStub()

        val result = GradleRunner.create()
            .withProjectDir(buildDir)
            .withArguments(
                "--stacktrace",
                "build","myPublishToGithubRelease")
            .build()
        result.dumpOutputOnError("myPublishToGithubRelease")
        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":myPublishToGithubRelease")?.outcome
        )
        result.assertMessageInOutput(
            "Uploaded '$localJarName' to release " +
                "'$releaseLabel' with asset name '$remoteJarName'.")
    }


    /**
     * Publishes a plugin jar to a normal release first, and then
     * to the pickup release; should also update the pickup release
     * description.
     */
    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun test_02(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri uri: String) {

        val minJosmVersion = 1111
        val releaseId = 12345678
        val releaseLabel = "v0.0.1"

        val githubConfig = githubConfig(uri)

        val pickupReleaseLabel = "most_recent"
        val pickupReleaseId = "45681234"
        val localJarName = "test-$releaseLabel.jar"
        val remoteJarName = "test.jar"

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
                id 'java'
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
                // publish to the pickup release task too
                publishToPickupRelease = true
            }
            """

        val releasesContent = """
              pickup_release_for_josm:
                label: $pickupReleaseLabel
                description: |
                  pickup-release-description
                  {{ labelForPickedUpRelease }}
                  {{ descriptionForPickedUpRelease }}
                
              releases:
                - label: $releaseLabel
                  numeric_josm_version: $minJosmVersion
                  description: $releaseLabel-description
            """.trimIndent()


        fun prepareAPIStub() {
            val leadingPath = "/repos/$GITHUB_USER/${githubConfig.repositoryName}/releases"

            // stub for "get releases"
            server.stubFor(get(urlPathEqualTo(leadingPath))
                .inScenario("upload-new-asset")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("""[
                        {
                            "id": $releaseId,
                            "tag_name": "$releaseLabel"
                        },
                        {
                            "id": $pickupReleaseId,
                            "tag_name": "$pickupReleaseLabel"
                        }
                    ]""".trimIndent())
                )
            )

            // stub for get release assets for normal release
            val path2 = "$leadingPath/$releaseId/assets"
            server.stubFor(get(urlPathEqualTo(path2))
                .inScenario("upload-new-assets")
                .willReturn(aResponse()
                    .withStatus(200)
                    // no existing release asset
                    .withBody("[]")
                )
            )

            // don't expect a delete request for an already existing
            // assets; there are none in this test case

            // stub for "upload release asset" to normal release
            val path3 = "$leadingPath/$releaseId/assets"
            server.stubFor(post(urlPathEqualTo(path3))
                .inScenario("upload-new-assets")
                .withHeader("Content-Type", equalTo(MEDIA_TYPE_JAR))
                .withQueryParam("name", equalTo(remoteJarName))
                //.withQueryParam("label", equalTo("test_plugin.jar"))
                .willReturn(aResponse()
                    .withStatus(201)
                    .withBody("""{"id": 1}""")
                )
            )


            // stub for get release assets for the pickup release
            val path4 = "$leadingPath/$pickupReleaseId/assets"
            server.stubFor(get(urlPathEqualTo(path4))
                .inScenario("upload-new-assets")
                .willReturn(aResponse()
                    .withStatus(200)
                    // no assets
                    .withBody("[]")
                )
            )

            // don't expect a delete request for an already existing
            // assets in the pickup release; there are none in this test case

            // stub for "update pickup release description"
            val path5 = "$leadingPath/$pickupReleaseId"
            server.stubFor(patch(urlPathEqualTo(path5))
                .inScenario("upload-new-assets")
                //TODO: the updated description in the body should match
                // with the expected value
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("""
                        {
                            "id": $pickupReleaseId,
                            "tag_name": "$pickupReleaseLabel"
                        }
                    """.trimIndent())
                )
            )

            // stub for "upload release asset" to the pickup release
            val path6 = "$leadingPath/$pickupReleaseId/assets"
            server.stubFor(post(urlPathEqualTo(path6))
                .inScenario("upload-new-assets")
                .withHeader("Content-Type", equalTo(MEDIA_TYPE_JAR))
                .withQueryParam("name", equalTo(remoteJarName))
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
            .withArguments(
                "build", "myPublishToGithubRelease",
                "--stacktrace"
            ).build()

        result.dumpOutputOnError("myPublishToGithubRelease")

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":myPublishToGithubRelease")?.outcome
        )
        result.assertMessageInOutput(
            "Uploaded '$localJarName' to release " +
            "'$releaseLabel' with asset name '$remoteJarName'.")

        result.assertMessageInOutput(
            "Uploaded '$localJarName' to release " +
            "'$pickupReleaseLabel' with asset name '$remoteJarName'.")
    }

    /**
     * Publish a plugin jar to a normal release using the standard task
     * and standard task configuration. Supply the release label on the
     * command line using '--release-label'.
     */
    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun test_03(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri uri: String) {

        val minJosmVersion = 1111
        val releaseId = 12345678
        val releaseLabel = "v0.0.1"

        val githubConfig = githubConfig(uri)

        // the standard pattern for the jar name the gradle-josm-plugin sets
        // for the plugin jar
        val localJarName = "${buildDir?.name}-$releaseLabel.jar"

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
                id 'java'
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
            """

        val releasesContent = """
              releases:
                - label: $releaseLabel
                  numeric_josm_version: $minJosmVersion
            """.trimIndent()



        fun prepareAPIStub() {
            // stub for "get releases"
            val leadingPath = "/repos/$GITHUB_USER/${githubConfig.repositoryName}/releases"
            server.stubFor(get(urlPathEqualTo(leadingPath))
                .inScenario("upload-asset")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(
                        """[{
                            "id": $releaseId,
                            "tag_name": "$releaseLabel"
                            }]""".trimIndent())
                )
            )

            // stub for get release assets for normal release
            val path2 = "$leadingPath/$releaseId/assets"
            server.stubFor(get(urlPathEqualTo(path2))
                .inScenario("upload-assets")
                .willReturn(aResponse()
                    .withStatus(200)
                    // no assets
                    .withBody("[]")
                )
            )

            // stub for "upload release asset"
            val path3 = "$leadingPath/$releaseId/assets"
            server.stubFor(post(urlPathEqualTo(path3))
                .inScenario("upload-assets")
                .withHeader("Content-Type", equalTo(MEDIA_TYPE_JAR))
                .withQueryParam("name", equalTo(localJarName))
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
            .withArguments(
                "--stacktrace",
                "build", "publishToGithubRelease",
                "--release-label", releaseLabel
            )
            .build()

        result.dumpOutputOnError("publishToGithubRelease")
        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":publishToGithubRelease")?.outcome
        )
        result.assertMessageInOutput(
            "Uploaded '$localJarName' to release " +
            "'$releaseLabel' with asset name '$localJarName'.")
    }

    /**
     * Publish a plugin jar to a normal release and to the pickup
     * release.
     * The release label is specified on the command line with
     * --release-label.
     * Uses --publish-to-pickup-release on the command line
     */
    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun test_04(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri uri: String){

        val minJosmVersion = 1111
        val releaseId = 12345678
        val releaseLabel = "v0.0.1"

        val githubConfig = githubConfig(uri)

        val pickupReleaseLabel = "most_recent"
        val pickupReleaseId = "45681234"

        val localJarName = "test-$releaseLabel.jar"
        val remoteJarName = "test.jar"

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
                id 'java'
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

            publishToGithubRelease {
                remoteJarName = "$remoteJarName"
            }
            """

        val releasesContent = """
            pickup_release_for_josm:
              label: $pickupReleaseLabel
              description: |
                pickup-release-description
                {{ labelForPickedUpRelease }}
                {{ descriptionForPickedUpRelease }}

            releases:
              - label: $releaseLabel
                numeric_josm_version: $minJosmVersion
            """.trimIndent()


        fun prepareAPIStub() {

            // stub for "get releases"
            val leadingPath = "/repos/$GITHUB_USER/${githubConfig.repositoryName}/releases"
            server.stubFor(get(urlPathEqualTo(leadingPath))
                .inScenario("upload-asset")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("""[
                        {
                            "id": $releaseId,
                            "tag_name": "$releaseLabel"
                        },
                        {
                            "id": $pickupReleaseId,
                            "tag_name": "$pickupReleaseLabel"
                        }
                    ]""".trimIndent())
                )
            )

            // stub for get release assets for normal release
            val path2 = "$leadingPath/$releaseId/assets"
            server.stubFor(get(urlPathEqualTo(path2))
                .inScenario("upload-assets")
                .willReturn(aResponse()
                    .withStatus(200)
                    // no assets
                    .withBody("[]")
                )
            )

            // stub for "upload release asset"
            val path3 = "$leadingPath/$releaseId/assets"
            server.stubFor(post(urlPathEqualTo(path3))
                .inScenario("upload-assets")
                .withHeader("Content-Type", equalTo(MEDIA_TYPE_JAR))
                .withQueryParam("name", equalTo(remoteJarName))
                .willReturn(aResponse()
                    .withStatus(201)
                    .withBody("""{"id": 1}""")
                )
            )

            // stub for get release assets for pickup release
            val path4 = "$leadingPath/$pickupReleaseId/assets"
            server.stubFor(get(urlPathEqualTo(path4))
                .inScenario("upload-assets")
                .willReturn(aResponse()
                    .withStatus(200)
                    // no assets
                    .withBody("[]")
                )
            )

            // stub for "update pickup release description"
            val path5 = "$leadingPath/$pickupReleaseId"
            server.stubFor(patch(urlPathEqualTo(path5))
                .inScenario("upload-assets")
                //TODO: the updated description in the body should match
                // with the expected value
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("""
                        {
                            "id": $pickupReleaseId,
                            "tag_name": "$pickupReleaseLabel"
                        }
                    """.trimIndent())
                )
            )

            // stub for "upload release asset" to the pickup release
            val path6 = "$leadingPath/$pickupReleaseId/assets"
            server.stubFor(post(urlPathEqualTo(path6))
                .inScenario("upload-assets")
                .withHeader("Content-Type", equalTo(MEDIA_TYPE_JAR))
                .withQueryParam("name", equalTo(remoteJarName))
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
            .withArguments(
                "build", "publishToGithubRelease",
                "--release-label", releaseLabel,
                "--publish-to-pickup-release",
                "--stacktrace"
                )
            .build()

        result.dumpOutputOnError("publishToGithubRelease")
        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":publishToGithubRelease")?.outcome
        )
        result.assertMessageInOutput(
            "Uploaded '$localJarName' to release " +
            "'$releaseLabel' with asset name '$remoteJarName'.")

        result.assertMessageInOutput(
            "Uploaded '$localJarName' to release " +
                "'$pickupReleaseLabel' with asset name '$remoteJarName'.")
    }
}
