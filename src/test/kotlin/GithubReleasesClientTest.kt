package org.openstreetmap.josm.gradle.plugin.ghreleases


import com.beust.klaxon.JsonObject
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockResolver.Wiremock
import ru.lanwen.wiremock.ext.WiremockUriResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver.WiremockUri

const val GITHUB_USER = "a-user"
const val GITHUB_ACCESS_TOKEN = "an-access-token"

class GithubReleasesClientTest {

    fun buildClient(apiUri: String) : GithubReleasesClient {
//        val accessToken = System.getenv("GITHUB_ACCESS_TOKEN")
//        val githubUser = System.getenv("GITHUB_USER")
//        if (accessToken == null) {
//            println("Warning: required environment variable "
//                + "GITHUB_ACCESS_TOKEN not set")
//        }
//        if (githubUser == null) {
//            println("Warning: required environment variable GITHUB_USER "
//                + "not set")
//        }
        val client = GithubReleasesClient()
        client.user = GITHUB_USER
        client.repository = "josm-scripting-plugin"
        client.accessToken = GITHUB_ACCESS_TOKEN
        client.apiUrl = apiUri
        return client
    }


    @Test
    fun `pagination with an empty Link header should work`() {
        val pagination = Pagination(null)
        assertNull(pagination.nextUrl)
    }

    @Test
    fun `pagination with a url of type rel="next" should work`() {
        val pagination = Pagination(
            "<https://api.github.com" +
            "/search/code?q=addClass+user%3Amozilla&page=15>; rel=\"next\"")
        assertEquals(
            pagination.nextUrl,
            "https://api.github.com" +
            "/search/code?q=addClass+user%3Amozilla&page=15"
        )
    }

    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `getLatestRelease should return a release for HTTP 200`(
        @Wiremock server: WireMockServer, @WiremockUri uri: String) {
        val client = buildClient(uri)
        val path = "/repos/${client.user}/${client.repository}/releases/latest"

        // replies two release in the first page
        server.stubFor(get(urlPathEqualTo(path))
            .willReturn(aResponse()
            .withStatus(200)
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
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class
    )
    fun `getLatestRelease should return null for HTTP 404`(
        @Wiremock server: WireMockServer, @WiremockUri uri: String) {
        val client = buildClient(uri)
        val path = "/repos/${client.user}/${client.repository}/releases/latest"

        // replies two release in the first page
        server.stubFor(get(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withStatus(404)
                // link to the next page of releases
                .withBody("""{"message": "not found"}""")
            )
        )
        val release = client.getLatestRelease()
        assertNull(release)
    }

    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `getLatestRelease should throw for an illegal http status code`(
        @Wiremock server: WireMockServer, @WiremockUri uri: String) {
        val client = buildClient(uri)
        val path = "/repos/${client.user}/${client.repository}/releases/latest"

        // replies two release in the first page
        server.stubFor(get(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withStatus(500)
                // link to the next page of releases
                .withBody("Server Error")
            )
        )
        assertThrows(GithubReleaseClientException::class.java, {
            client.getLatestRelease()
        })
    }

    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `createRelease with only a tag name should work`(
        @Wiremock server: WireMockServer, @WiremockUri uri: String) {
        val client = buildClient(uri)
        val path = "/repos/${client.user}/${client.repository}/releases"

        val tagName = "v0.0.1"

        // replies two release in the first page
        server.stubFor(post(urlPathEqualTo(path))
          .withRequestBody(matchingJsonPath("$[?(@.tag_name == '${tagName}')]"))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody("""{"id": 1}""")
            )
        )
        val newRelease = client.createRelease(tagName)
        assertEquals(newRelease["id"], 1)
    }

    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `createRelease should accept optional parameters`(
        @Wiremock server: WireMockServer, @WiremockUri uri: String) {
        val client = buildClient(uri)
        val path = "/repos/${client.user}/${client.repository}/releases"

        val tagName = "v0.0.1"
        val name = "aname"
        val targetCommitish = "acommitish"
        val body = "abody"

        // only included in the request body if different from default value,
        // i.e. false
        val draft = false
        // only included in the request body if different from default value,
        // i.e. false
        val prerelease = false

        // replies two release in the first page
        server.stubFor(post(urlPathEqualTo(path))
            .withRequestBody(matchingJsonPath(
                "$[?(@.tag_name == '$tagName')]"))
            .withRequestBody(matchingJsonPath("$[?(@.name == '$name')]"))
            .withRequestBody(matchingJsonPath(
                "$[?(@.target_commitish == '$targetCommitish')]"))
            .withRequestBody(matchingJsonPath("$[?(@.body == '$body')]"))
            .withRequestBody(matchingJsonPath("$[?(@.draft == false)]"))
            .withRequestBody(matchingJsonPath("$[?(@.prerelease == false)]"))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody("""{"id": 1}""")
            )
        )
        val newRelease = client.createRelease(tagName, name = name,
            targetCommitish = targetCommitish, body = body, draft = draft,
            prerelease = prerelease)
        assertEquals(newRelease["id"], 1)

    }

    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `if a few releases are present, getReleases should work`(
        @Wiremock server: WireMockServer, @WiremockUri uri: String) {
        val client = buildClient(uri)
        val path = "/repos/${client.user}/${client.repository}/releases"

        // replies two release in the first page
        server.stubFor(get(urlPathEqualTo(path))
            .inScenario("paging")
            .willReturn(aResponse()
                .withStatus(200)
                // link to the next page of releases
                .withHeader("Link",
                    "<${client.apiUrl}${path}?page=2>; rel=\"next\"")
                .withBody("""[
                    {"id": 1},
                    {"id": 2}
                    ]""")
                )
            )
        // replies two more releases in the second page
        server.stubFor(get(urlPathEqualTo(path))
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
        val releases = client.getReleases()
        assertEquals(releases.size, 4)
    }

    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `uploading a simple text file as release asset should work`(
        @Wiremock server: WireMockServer, @WiremockUri uri: String) {
        val client = buildClient(uri)
        val releaseId = 12345
        val path = "/repos/${client.user}/${client.repository}" +
                    "/releases/${releaseId}/assets"
        val asset = createTempFile(suffix="txt")
        val content = "Hello World!"
        asset.writeText(content)

        // replies two release in the first page
        server.stubFor(post(urlPathEqualTo(path))
            .withRequestBody(equalTo(content))
            .withHeader("Content-Type", equalTo("text/plain"))
            .willReturn(aResponse()
                .withStatus(201)
                .withBody("""{"id": 1}""")
            )
        )

        val assets = client.uploadReleaseAsset(releaseId=releaseId,
            contentType = "text/plain", file = asset)
        assertEquals(assets.size, 1)
    }

    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun `uploading a simple text file + name and label should work`(
        @Wiremock server: WireMockServer, @WiremockUri uri: String) {
        val client = buildClient(uri)
        val releaseId = 12345
        val asset = createTempFile(suffix="txt")
        val content = "Hello World!"
        asset.writeText(content)
        val newName = "asset.txt"
        val label = "This is a label"
        val path = "/repos/${client.user}/${client.repository}" +
                    "/releases/${releaseId}/assets"

        server.stubFor(post(urlPathMatching("${path}.*"))
            .withRequestBody(equalTo(content))
            .withHeader("Content-Type", equalTo("text/plain"))
            .withQueryParam("name", equalTo(newName))
            .withQueryParam("label", equalTo(label))
            .willReturn(aResponse()
                .withStatus(201)
                .withBody("""{"id": 1}""")
            )
        )

        val assets = client.uploadReleaseAsset(releaseId = releaseId,
            contentType = "text/plain", file = asset, name = newName,
            label = label)
        assertEquals(assets.size, 1)

    }

    @Test
    @ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
    fun testGetReleases(@Wiremock server: WireMockServer,
                        @WiremockUri uri: String) {
        val client = buildClient(uri)
        val url = "/repos/${client.user}/${client.repository}/releases"

        server.stubFor(get(urlEqualTo(url))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("""[
                    {"id": 1},
                    {"id": 2}
                    ]""")
                )
            )

        client.getReleases()
    }
}
