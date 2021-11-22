package org.openstreetmap.josm.gradle.plugin.github

import com.beust.klaxon.JsonObject
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.openstreetmap.josm.gradle.plugin.testutils.GradleProjectUtil
import org.openstreetmap.josm.gradle.plugin.util.Urls
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK

const val GITHUB_USER = "a-user"
const val GITHUB_ACCESS_TOKEN = "an-access-token"

@WireMockTest
class GithubReleasesClientTest {

    private fun buildClient(apiUri: String) = GithubReleasesClient(
      "josm-scripting-plugin",
      GITHUB_USER,
      GITHUB_ACCESS_TOKEN,
      apiUri
    )

    @Test
    fun `pagination with an empty Link header should work`() {
        val pagination = Pagination(null)
        assertNull(pagination.nextUrl)
    }

    @Test
    fun `pagination with a url of type rel="next" should work`() {
        val pagination = Pagination(
          "<${Urls.Github.API}/search/code?q=addClass+user%3Amozilla&page=15>; rel=\"next\"")
        assertEquals(
            pagination.nextUrl,
            "${Urls.Github.API}/search/code?q=addClass+user%3Amozilla&page=15"
        )
    }

    @Test
    fun `getLatestRelease should return a release for HTTP 200`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = buildClient(wmRuntimeInfo.httpBaseUrl)
        val path = "/repos/${client.user}/${client.repository}/releases/latest"

        // replies two release in the first page
        wmRuntimeInfo.wireMock.register(get(urlPathEqualTo(path))
            .willReturn(aResponse()
            .withStatus(HTTP_OK)
            // link to the next page of releases
            .withBody("""{"id": 1}""")
            )
        )
        val release = client.getLatestRelease()
        assertNotNull(release)
        assertTrue(release is JsonObject)
        assertEquals(release?.int("id"), 1)
    }

    @Test
    fun `getLatestRelease should return null for HTTP 404`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = buildClient(wmRuntimeInfo.httpBaseUrl)
        val path = "/repos/${client.user}/${client.repository}/releases/latest"

        // replies two release in the first page
        wmRuntimeInfo.wireMock.register(get(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withStatus(HTTP_NOT_FOUND)
                // link to the next page of releases
                .withBody("""{"message": "not found"}""")
            )
        )
        val release = client.getLatestRelease()
        assertNull(release)
    }

    @Test
    fun `getLatestRelease should throw for an illegal http status code`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = buildClient(wmRuntimeInfo.httpBaseUrl)
        val path = "/repos/${client.user}/${client.repository}/releases/latest"

        // replies two release in the first page
        wmRuntimeInfo.wireMock.register(get(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withStatus(HTTP_INTERNAL_ERROR)
                // link to the next page of releases
                .withBody("Server Error")
            )
        )
        assertThrows(GithubReleaseException::class.java) {
            client.getLatestRelease()
        }
    }

    @Test
    fun `createRelease with only a tag name should work`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = buildClient(wmRuntimeInfo.httpBaseUrl)
        val path = "/repos/${client.user}/${client.repository}/releases"

        val tagName = "v0.0.1"

        // replies two release in the first page
        wmRuntimeInfo.wireMock.register(post(urlPathEqualTo(path))
          .withRequestBody(matchingJsonPath("$[?(@.tag_name == '$tagName')]"))
            .willReturn(aResponse()
              .withStatus(HTTP_CREATED)
              .withBody("""{"id": 1}""")
            )
        )
        val newRelease = client.createRelease(tagName)
        assertEquals(newRelease["id"], 1)
    }

    @Test
    fun `createRelease should accept optional parameters`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = buildClient(wmRuntimeInfo.httpBaseUrl)
        val path = "/repos/${client.user}/${client.repository}/releases"

        val tagName = "v0.0.1"
        val name = "aname"
        val targetCommitish = "acommitish"
        val body = "abody"

        // only included in the request body if set to true (false is the default)
        val draft = true
        // only included in the request body if set to true (false is the default)
        val prerelease = true

        // replies two release in the first page
        wmRuntimeInfo.wireMock.register(post(urlPathEqualTo(path))
            .withRequestBody(matchingJsonPath(
                "$[?(@.tag_name == '$tagName')]"))
            .withRequestBody(matchingJsonPath("$[?(@.name == '$name')]"))
            .withRequestBody(matchingJsonPath(
                "$[?(@.target_commitish == '$targetCommitish')]"))
            .withRequestBody(matchingJsonPath("$[?(@.body == '$body')]"))
            .withRequestBody(matchingJsonPath("$[?(@.draft == true)]"))
            .withRequestBody(matchingJsonPath("$[?(@.prerelease == true)]"))
            .willReturn(aResponse()
              .withStatus(HTTP_CREATED)
              .withBody("""{"id": 1}""")
            )
        )
        val newRelease = client.createRelease(tagName, name = name,
            targetCommitish = targetCommitish, body = body, draft = draft,
            prerelease = prerelease)
        assertEquals(newRelease["id"], 1)
    }

    @Test
    fun `updateRelease updating all attributes should work`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = buildClient(wmRuntimeInfo.httpBaseUrl)
        val releaseId = 123456

        val tagName = "new-tag"
        val targetCommitish = "new-target-commitish"
        val name = "new-name"
        val body = "new-body"
        val draft = true
        val prerelease = true

        val path = "/repos/${client.user}/${client.repository}/releases" +
            "/$releaseId"

        wmRuntimeInfo.wireMock.register(patch(urlPathEqualTo(path))
            .withRequestBody(matchingJsonPath(
                "$[?(@.tag_name == '$tagName')]"))
            .withRequestBody(matchingJsonPath("$[?(@.name == '$name')]"))
            .withRequestBody(matchingJsonPath(
                "$[?(@.target_commitish == '$targetCommitish')]"))
            .withRequestBody(matchingJsonPath("$[?(@.body == '$body')]"))
            .withRequestBody(matchingJsonPath("$[?(@.draft == $draft)]"))
            .withRequestBody(matchingJsonPath(
                "$[?(@.prerelease == $prerelease)]"))
            .willReturn(aResponse()
                .withStatus(HTTP_OK)
                .withBody("""{"id": $releaseId}""")
            )
        )
        val updatedRelease = client.updateRelease(
            releaseId,
            tagName = tagName, name = name, targetCommitish = targetCommitish,
            body = body, draft = draft, prerelease = prerelease)
        assertEquals(releaseId, updatedRelease["id"])
    }

    @Test
    fun `updateRelease updating body only should work`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = buildClient(wmRuntimeInfo.httpBaseUrl)
        val releaseId = 123456
        val body = "new-body"

        val path = "/repos/${client.user}/${client.repository}/releases" +
            "/$releaseId"

        wmRuntimeInfo.wireMock.register(patch(urlPathEqualTo(path))
            .withRequestBody(matchingJsonPath("$[?(@.body == '$body')]"))
            .willReturn(aResponse()
                .withStatus(HTTP_OK)
                .withBody("""{"id": $releaseId}""")
            )
        )
        val updatedRelease = client.updateRelease(
            releaseId, body = body)
        assertEquals(releaseId, updatedRelease["id"])
    }

    @Test
    fun `if a few releases are present, getReleases should work`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = buildClient(wmRuntimeInfo.httpBaseUrl)
        val path = "/repos/${client.user}/${client.repository}/releases"

        // replies two release in the first page
        wmRuntimeInfo.wireMock.register(get(urlPathEqualTo(path))
            .inScenario("paging")
            .willReturn(aResponse()
                .withStatus(HTTP_OK)
                // link to the next page of releases
                .withHeader("Link",
                    "<${client.apiUrl}$path?page=2>; rel=\"next\"")
                .withBody("""[
                    {"id": 1},
                    {"id": 2}
                    ]""")
                )
            )
        // replies two more releases in the second page
        wmRuntimeInfo.wireMock.register(get(urlPathEqualTo(path))
            .withQueryParam("page", equalTo("2"))
            .inScenario("paging")
            .willReturn(aResponse()
                .withStatus(HTTP_OK)
                .withBody("""[
                    {"id": 3},
                    {"id": 4}
                    ]""")
                )
            )
        val releases = client.getReleases()
        assertEquals(releases.size, 4)
    }

    @Test
    fun `uploading a simple text file as release asset should work`(
      testInfo: TestInfo,
      wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = buildClient(wmRuntimeInfo.httpBaseUrl)
        val releaseId = 12345
        val path = "/repos/${client.user}/${client.repository}" +
                    "/releases/$releaseId/assets"
        val asset = GradleProjectUtil.createTempFile(testInfo, suffix=".txt")
        val content = "Hello World!"
        asset.writeText(content)

        // replies two release in the first page
        wmRuntimeInfo.wireMock.register(post(urlPathEqualTo(path))
            .withRequestBody(equalTo(content))
            .withHeader("Content-Type", equalTo("text/plain"))
            .willReturn(aResponse()
                .withStatus(HTTP_CREATED)
                .withBody("""{"id": 1}""")
            )
        )

        val assets = client.uploadReleaseAsset(releaseId=releaseId,
            contentType = "text/plain", file = asset)
        assertEquals(assets.size, 1)
    }

    @Test
    fun `uploading a simple text file + name and label should work`(
      testInfo: TestInfo,
      wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = buildClient(wmRuntimeInfo.httpBaseUrl)
        val releaseId = 12345
        val asset = GradleProjectUtil.createTempFile(testInfo, suffix="txt")
        val content = "Hello World!"
        asset.writeText(content)
        val newName = "asset.txt"
        val label = "This is a label"
        val path = "/repos/${client.user}/${client.repository}" +
                    "/releases/$releaseId/assets"

        wmRuntimeInfo.wireMock.register(post(urlPathMatching("$path.*"))
            .withRequestBody(equalTo(content))
            .withHeader("Content-Type", equalTo("text/plain"))
            .withQueryParam("name", equalTo(newName))
            .withQueryParam("label", equalTo(label))
            .willReturn(aResponse()
                .withStatus(HTTP_CREATED)
                .withBody("""{"id": 1}""")
            )
        )

        val assets = client.uploadReleaseAsset(releaseId = releaseId,
            contentType = "text/plain", file = asset, name = newName,
            label = label)
        assertEquals(assets.size, 1)
    }

    @Test
    fun testGetReleases(wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = buildClient(wmRuntimeInfo.httpBaseUrl)
        val url = "/repos/${client.user}/${client.repository}/releases"

        wmRuntimeInfo.wireMock.register(get(urlEqualTo(url))
            .willReturn(aResponse()
                .withStatus(HTTP_OK)
                .withBody("""[
                    {"id": 1},
                    {"id": 2}
                    ]""")
                )
            )

        client.getReleases()
    }

    @Test
    fun `getReleaseAssets() with one page should work`(
                        wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = buildClient(wmRuntimeInfo.httpBaseUrl)
        val releaseId = 123456
        val url = "/repos/${client.user}/${client.repository}/releases" +
            "/$releaseId/assets"

        wmRuntimeInfo.wireMock.register(get(urlEqualTo(url))
            .willReturn(aResponse()
                .withStatus(HTTP_OK)
                // no link header
                .withBody("""[
                    {"id": 1},
                    {"id": 2}
                    ]""")

            )
        )

        val assets = client.getReleaseAssets(releaseId = releaseId)
        assertEquals(2, assets.size)
        assertEquals(IntRange(1,2).toList(),
            assets.map {it["id"].toString().toInt()}.sorted()
        )
    }

    @Test
    fun `getReleaseAssets() with two pages should work`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = buildClient(wmRuntimeInfo.httpBaseUrl)
        val releaseId = 123456
        val path = "/repos/${client.user}/${client.repository}/releases" +
            "/$releaseId/assets"

        // replies two assets for the first page
        wmRuntimeInfo.wireMock.register(get(urlPathEqualTo(path))
            .inScenario("paging")
            .willReturn(aResponse()
                .withStatus(HTTP_OK)
                // link to the next page of releases
                .withHeader("Link",
                    "<${client.apiUrl}$path?page=2>; rel=\"next\"")
                .withBody("""[
                    {"id": 1},
                    {"id": 2}
                    ]""")
            )
        )

        // replies two more assets in the second page
        wmRuntimeInfo.wireMock.register(get(urlPathEqualTo(path))
            .withQueryParam("page", equalTo("2"))
            .inScenario("paging")
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("""[
                    {"id": 3},
                    {"id": 4}
                    ]""")
            )
        )

        val assets = client.getReleaseAssets(releaseId = releaseId)
        assertEquals(4, assets.size)
        assertEquals(IntRange(1,4).toList(),
            assets.map {it["id"].toString().toInt()}.sorted()
        )
    }

    @Test
    fun `getReleaseAssets() with no assets should work`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = buildClient(wmRuntimeInfo.httpBaseUrl)
        val releaseId = 123456
        val path = "/repos/${client.user}/${client.repository}/releases" +
            "/$releaseId/assets"

        // replies two assets for the first page
        wmRuntimeInfo.wireMock.register(get(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withStatus(HTTP_OK)
                .withBody("[]")
            )
        )

        val assets = client.getReleaseAssets(releaseId = releaseId)
        assertEquals(0, assets.size)
    }


    @Test
    fun `deleteReleaseAsset() for existing asset should work`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = buildClient(wmRuntimeInfo.httpBaseUrl)
        val assetId = 123456
        val path = "/repos/${client.user}/${client.repository}/releases" +
            "/assets/$assetId"

        // replies two assets for the first page
        wmRuntimeInfo.wireMock.register(delete(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withStatus(HTTP_NO_CONTENT)
                .withBody("")
            )
        )
        client.deleteReleaseAsset(assetId = assetId)
    }

    @Test
    fun `deleteReleaseAsset() for non existing asset should work`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = buildClient(wmRuntimeInfo.httpBaseUrl)
        // assumption: there's no asset with this id
        val assetId = 123456
        val path = "/repos/${client.user}/${client.repository}/releases" +
            "/assets/$assetId"

        // replies two assets for the first page
        wmRuntimeInfo.wireMock.register(delete(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withStatus(HTTP_NOT_FOUND)
                .withBody("""
                    {
                        "message": "Not Found",
                        "documentation_url": "https://an-url/"
                    }
                """.trimIndent())
            )
        )
        assertThrows(GithubReleaseException::class.java) {
            client.deleteReleaseAsset(assetId = assetId)
        }
    }
}
