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
import org.openstreetmap.josm.gradle.plugin.task.ENV_VAR_GITHUB_USER
import org.openstreetmap.josm.gradle.plugin.task.MEDIA_TYPE_JAR
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver
import java.io.File


class PublishToGithubReleaseTaskTest() : BaseGithubReleaseTaskTest() {

    private fun prepareTestPluginSource() {
        val testPluginContent = """
                package test_plugin;
                import org.openstreetmap.josm.plugins.Plugin;
                import org.openstreetmap.josm.plugins.PluginInformation;
                public class TestPlugin extends Plugin {
                  public TestPlugin(PluginInformation info) {
                      super(info);
                  }
                }
            """.trimIndent()
        val sourceDir = File(buildDir, "src/main/java/test_plugin")
        sourceDir.mkdirs()

        val sourceFile = File(sourceDir, "TestPlugin.java")
        sourceFile?.printWriter()?.use { out ->
            out.println(testPluginContent)
        }
    }

    private fun prepareBuildFile(content: String) {
        buildFile?.printWriter()?.use { out ->
            out.println(content)
        }
    }

    private fun prepareReleasesSpecs(content: String,
                                     releasesFile: File? = null) {
        val releasesFile = releasesFile ?: File(buildDir, "releases.yml")
        releasesFile?.printWriter()?.use { it.println(content) }
    }

    @Test
    @Disabled
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `with minimal configuration values - supplied on the command line`(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri uri: String) {

        val minJosmVersion = 1111
        val pluginVersion = 2222
        val releaseId = 12345678
        val releaseLabel = "v0.0.1"
        val githubRepo = "repo_xy"

        // the standard pattern for the jar name the gradle-josm-plugin sets
        // for the plugin jar
        val localJarName = "${buildDir?.name}-$pluginVersion.jar"

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
                id 'java'
            }
            version = $pluginVersion
            josm {
              josmCompileVersion = "latest"
              manifest {
                  description = 'test'
                  minJosmVersion = $minJosmVersion
                  mainClass = 'test_plugin.TestPlugin'
              }
            }

            import org.openstreetmap.josm.gradle.plugin.task.PublishToGithubReleaseTask
            task publishToGithubRelease(type: PublishToGithubReleaseTask){
                // no configuration. use standard values
            }
            """

        val releasesContent = """
              releases:
                - label: $releaseLabel
                  numeric_plugin_version: $pluginVersion
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
                "--release-label", releaseLabel,
                "--github-api-url", uri,
                "--github-repository", githubRepo
            )
        .build()
        Assertions.assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":publishToGithubRelease")?.outcome
        )
    }

    @Test
    @Disabled
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `configured with task parameters, with custom remote jar name`(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri uri: String
    ) {

        val minJosmVersion = 1111
        val pluginVersion = 2222
        val releaseId = 12345678
        val releaseLabel = "v0.0.1"
        val githubRepo = "repo_xy"

        val localJarName = "test-$pluginVersion.jar"
        val remoteJarName = "test.jar"
        val localJarPath = "${buildDir?.absolutePath}/build/dist/$localJarName"

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
                id 'java'
            }
            version = $pluginVersion
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
            task publishToGithubRelease(type: PublishToGithubReleaseTask){
                githubApiUrl = "$uri"
                githubRepository = "$githubRepo"
                releaseLabel = "$releaseLabel"
                remoteJarName = "$remoteJarName"
            }
            """

        val releasesContent = """
              releases:
                - label: $releaseLabel
                  numeric_plugin_version: $pluginVersion
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
            .withArguments("build", "publishToGithubRelease")
            .build()
        Assertions.assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":publishToGithubRelease")?.outcome
        )
    }


    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `should publish to latest release too`(
        @WiremockResolver.Wiremock server: WireMockServer,
        @WiremockUriResolver.WiremockUri uri: String
    ) {

        val minJosmVersion = 1111
        val pluginVersion = 2222
        val releaseId = 12345678
        val releaseLabel = "v0.0.1"
        val githubRepo = "repo_xy"

        val latestReleaseLabel = "most_recent"
        val latestReleaseId = "45681234"

        val localJarName = "test-$pluginVersion.jar"
        val remoteJarName = "test.jar"

        val buildFileContent = """
            plugins {
                id 'org.openstreetmap.josm' version '${pluginUnderTestVersion()}'
                id 'java'
            }
            version = $pluginVersion
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
            task publishToGithubRelease(type: PublishToGithubReleaseTask){
                githubApiUrl = "$uri"
                githubRepository = "$githubRepo"
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
                  numeric_plugin_version: $pluginVersion
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