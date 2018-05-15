package org.openstreetmap.josm.gradle.plugin

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.powermock.api.mockito.PowerMockito
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver
import java.io.File

const val RELEASE_VERSION = "0.3.4-8-g16f093e-dirty"

class CreateGithubReleaseTaskTest {

  private var buildFile: File? = null
  private var buildDir: File? = null

  @BeforeEach
  fun setup() {
    buildDir = createTempDir("/tmp")
    buildFile = File(buildDir, "build.gradle")
    File(buildDir, "settings.gradle").printWriter()?.use {
      it.println("""
              pluginManagement {
                repositories {
                    maven {
                      url "${System.getenv("HOME")}/.m2/repository"
                    }
                    gradlePluginPortal()
                }
              }""".trimIndent()
      )
    }
  }

  //@AfterEach
  fun tearDown() {
    buildDir?.deleteRecursively()
  }

  @Test
  @Disabled
  fun `can execute a CreateGithubReleaseTask`() {
    val buildFileContent = """
        plugins {
            id 'org.openstreetmap.josm' version '${RELEASE_VERSION}'
        }

        import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask
        task createGithubRelease(type: CreateGithubReleaseTask){
        }
        """
    buildFile?.printWriter()?.use {out ->
      out.println(buildFileContent)
    }

    val result = GradleRunner.create()
      .withProjectDir(File("/tmp"))
      .withArguments("createGithubRelease")
      .build()
    assertEquals(SUCCESS, result.task(":createGithubRelease")?.getOutcome())
  }


  @Test
  @Disabled
  fun `can configure release label`() {
    val buildFileContent = """
        plugins {
            id 'org.openstreetmap.josm' version '${RELEASE_VERSION}'
        }

        import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask
        task createGithubRelease(type: CreateGithubReleaseTask){
          releaseLabel = "v0.0.1"
          doLast {
            print(releaseLabel)
          }
        }
        """
    buildFile?.printWriter()?.use {out ->
      out.println(buildFileContent)
    }

    val result = GradleRunner.create()
      .withProjectDir(File("/tmp"))
      .withArguments("createGithubRelease")
      .build()
    assertEquals(SUCCESS, result.task(":createGithubRelease")?.getOutcome())
    assertTrue(result.getOutput().contains("v0.0.1"))
  }

  fun runTestBuildForGithubUserConfiguration(build: String, arguments: List<String>) {
    buildFile?.printWriter()?.use {out ->
      out.println(build)
    }

    val result = GradleRunner.create()
      .withProjectDir(File("/tmp"))
      .withArguments(arguments)
      .build()
    assertEquals(SUCCESS, result.task(":createGithubRelease")?.getOutcome())
    assertTrue(result.getOutput().contains("a_github_user"))
  }

  @Test
  @Disabled
  fun `can configure github user via task configuration`() {
    val buildFileContent = """
        plugins {
            id 'org.openstreetmap.josm' version '${RELEASE_VERSION}'
        }

        import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask
        task createGithubRelease(type: CreateGithubReleaseTask){
          githubUser = "a_github_user"
          doLast {
            print(configuredGithubUser)
          }
        }
        """
    runTestBuildForGithubUserConfiguration(buildFileContent, listOf("createGithubRelease"))
  }

  @Test
  @Disabled
  fun `can configure github user via project property`() {
    val buildFileContent = """
        plugins {
            id 'org.openstreetmap.josm' version '${RELEASE_VERSION}'
        }

        project.ext.github_user = "a_github_user"

        import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask
        task createGithubRelease(type: CreateGithubReleaseTask){
          doLast {
            print(configuredGithubUser)
          }
        }
        """
    runTestBuildForGithubUserConfiguration(buildFileContent, listOf("createGithubRelease"))
  }

  @Test
  @Disabled
  //TODO: fix this. mocking a static method doesn't work
  fun `can configure github user via environment variable`() {
    PowerMockito.mockStatic(System::class.java)
    PowerMockito.`when`(System.getenv("GITHUB_USER")).thenReturn("a_github_user")

    val buildFileContent = """
        plugins {
            id 'org.openstreetmap.josm' version '${RELEASE_VERSION}'
        }

        import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask

        task createGithubRelease(type: CreateGithubReleaseTask){
          doLast {
            print(configuredGithubUser)
          }
        }
        """
    buildFile?.printWriter()?.use {out ->
      out.println(buildFileContent)
    }

    val result = GradleRunner.create()
      .withProjectDir(File("/tmp"))
      .withArguments("createGithubRelease")
      .build()
    assertEquals(SUCCESS, result.task(":createGithubRelease")?.getOutcome())
    assertTrue(result.getOutput().contains("a_github_user"))
  }


  @Test
  @Disabled
  fun `can configure github user via project property on command line`() {
    val buildFileContent = """
        plugins {
            id 'org.openstreetmap.josm' version '${RELEASE_VERSION}'
        }

        import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask
        task createGithubRelease(type: CreateGithubReleaseTask){
          doLast {
            print(configuredGithubUser)
          }
        }
        """
    runTestBuildForGithubUserConfiguration(buildFileContent,
      listOf("-Pgithub_user=a_github_user", "createGithubRelease"))
  }


  @Test
  @Disabled
  fun `can configure github user via task argument`() {
    val buildFileContent = """
        plugins {
            id 'org.openstreetmap.josm' version '${RELEASE_VERSION}'
        }

        import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask
        task createGithubRelease(type: CreateGithubReleaseTask){
          doLast {
            print(configuredGithubUser)
          }
        }
        """
    runTestBuildForGithubUserConfiguration(buildFileContent,
      listOf("createGithubRelease", "--github-user", "a_github_user"))
  }


  @Test
  @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
  @Disabled
  fun `can create a release, when arguments are configured in the task`(
    @WiremockResolver.Wiremock server: WireMockServer,
    @WiremockUriResolver.WiremockUri apiUri: String) {

    val releaseName = "a test release"
    val releaseLabel = "v0.0.2"

    val releaseFileContent = """
      releases:
        - label: "$releaseLabel"
          name: "$releaseName"
          numeric_plugin_version: 1234
          numeric_josm_version: 5678
          description: a test description
      """.trimIndent()
    File(buildDir, "releases.yml").printWriter()?.use {
      it.println(releaseFileContent)
    }

    val githubUser = "github_user"
    val githubAccessToken = "aaaabbbb"
    val githubRepo = "repo"

    val buildFileContent = """
        plugins {
            id 'org.openstreetmap.josm' version '$RELEASE_VERSION'
        }

        import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask

        task createGithubRelease(type: CreateGithubReleaseTask){
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
      )
    )

    server.stubFor(WireMock.post(WireMock.urlPathEqualTo(path))
      .withRequestBody(WireMock.matchingJsonPath("$[?(@.tag_name == '$releaseLabel')]"))
      .withRequestBody(WireMock.matchingJsonPath("$[?(@.name == '$releaseName')]"))
      .withBasicAuth(githubUser, githubAccessToken)
      .willReturn(WireMock.aResponse()
        .withStatus(200)
        .withBody("""{"id": 1}""")
      )
    )

    val result = GradleRunner.create()
      .withProjectDir(buildDir)
      .withArguments("createGithubRelease")
      .build()
    assertEquals(SUCCESS, result.task(":createGithubRelease")?.outcome)
    println(result.output)
  }

  @Test
  @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
  @Disabled
  fun `can create a release, when arguments are configured on the command line`(
      @WiremockResolver.Wiremock server: WireMockServer,
      @WiremockUriResolver.WiremockUri apiUri: String) {

    val releaseName = "a test release"
    val releaseLabel = "v0.0.2"

    val relaeaseFileContent = """
      releases:
        - label: "${releaseLabel}"
          name: "${releaseName}"
          numeric_plugin_version: 1234
          numeric_josm_version: 5678
          description: a test description
      """.trimIndent()
    File(buildDir, "releases.yml").printWriter()?.use {
      it.println(relaeaseFileContent)
    }

    val githubUser = "github_user"
    val githubAccessToken = "aaaabbbb"
    val githubRepo = "repo"

    val buildFileContent = """
        plugins {
            id 'org.openstreetmap.josm' version '${RELEASE_VERSION}'
        }

        import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask

        task createGithubRelease(type: CreateGithubReleaseTask){
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
      )
    )

    server.stubFor(WireMock.post(WireMock.urlPathEqualTo(path))
      .withRequestBody(WireMock.matchingJsonPath("$[?(@.tag_name == '$releaseLabel')]"))
      .withRequestBody(WireMock.matchingJsonPath("$[?(@.name == '$releaseName')]"))
      .withBasicAuth(githubUser, githubAccessToken)
      .willReturn(WireMock.aResponse()
        .withStatus(200)
        .withBody("""{"id": 1}""")
      )
    )

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
  fun `can create a release, when arguments are configured in project properties`(
    @WiremockResolver.Wiremock server: WireMockServer,
    @WiremockUriResolver.WiremockUri apiUri: String) {

    val releaseName = "a test release"
    val releaseLabel = "v0.0.2"

    val releaseFileContent = """
      releases:
        - label: "$releaseLabel"
          name: "$releaseName"
          numeric_plugin_version: 1234
          numeric_josm_version: 5678
          description: a test description
      """.trimIndent()
    File(buildDir, "releases.yml").printWriter()?.use {
      it.println(releaseFileContent)
    }

    val githubUser = "github_user"
    val githubAccessToken = "aaaabbbb"
    val githubRepo = "repo"

    val gradlePropertiesContent = """
        org.openstreetmap.josm.gradle.plugin.github_user = $githubUser
        org.openstreetmap.josm.gradle.plugin.github_access_token = $githubAccessToken
        org.openstreetmap.josm.gradle.plugin.github_repository = $githubRepo
        org.openstreetmap.josm.gradle.plugin.github_api_url = $apiUri
        """.trimIndent()
    File(buildDir, "gradle.properties").printWriter()?.use {
      it.println(gradlePropertiesContent)
    }

    val buildFileContent = """
        plugins {
            id 'org.openstreetmap.josm' version '${RELEASE_VERSION}'
        }

        import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask

        task createGithubRelease(type: CreateGithubReleaseTask){
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
      )
    )

    server.stubFor(WireMock.post(WireMock.urlPathEqualTo(path))
      .withRequestBody(WireMock.matchingJsonPath("$[?(@.tag_name == '$releaseLabel')]"))
      .withRequestBody(WireMock.matchingJsonPath("$[?(@.name == '$releaseName')]"))
      .withBasicAuth(githubUser, githubAccessToken)
      .willReturn(WireMock.aResponse()
        .withStatus(200)
        .withBody("""{"id": 1}""")
      )
    )

    val result = GradleRunner.create()
      .withProjectDir(buildDir)
      .withArguments("createGithubRelease")
      .build()
    assertEquals(SUCCESS, result.task(":createGithubRelease")?.outcome)
    println(result.output)
  }
}



