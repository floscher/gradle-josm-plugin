package org.openstreetmap.josm.gradle.plugin.ghreleases

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import okhttp3.*
import java.io.File
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_OK
import java.net.URLEncoder

// the default API URL for the GitHub API
const val DEFAULT_GITHUB_API_URL = "https://api.github.com"
// the default upload URL to upload a release asset
const val DEFAULT_GITHUB_UPLOAD_URL = "https://uploads.github.com"
// the default base URL to access a release
const val DEFAULT_GITHUB_URL = "http://github.com"


@Throws(GithubReleaseClientException::class)
private fun Response.toFormattedErrorMessage() : String {
    val body = this.body()?.string()
        ?: throw GithubReleaseClientException(
            "Unexpected error response body from GitHub API"
        )
    return try {
        Parser().parse(StringBuilder(body)) as JsonObject
    } catch(t: Throwable) {
        null
    }?.toJsonString(prettyPrint = true) ?: body
}

class GithubReleaseClientException(override var message: String,
                                   override var cause: Throwable?)
    : Exception(message, cause) {
    constructor(message: String) : this(message, null)
    constructor(response: Response) : this(
        """"Unexpected response from GitHub API.
            | Code: ${response.code()}.
            | Response body:
            | ${response.body()?.string() ?: ""}""".trimMargin("|"),
        null
    )
    constructor(e: Throwable): this(e.message ?: "", e)
}

/**
 * Information about related content pages in a sequence of content
 * pages returned by an API method.
 *
 * Limited in functionality. Only considers paging links of type "next"
*/
class Pagination(linkHeader: String?) {

    private var _nextUrl: String? = null

    /**
    *  Parses the relation URLs in `linkHeader` (if not null). Pass in the
    *  header value only, without the header name `Link:`
    *
    * Sample of a link header (including the Header name) replied by the Github
    * API
    * ```
    * Link: <https://api.github.com/search/code?q=addClass+user%3Amozilla&page=15>; rel="next",
    * <https://api.github.com/search/code?q=addClass+user%3Amozilla&page=34>; rel="last",
    * <https://api.github.com/search/code?q=addClass+user%3Amozilla&page=1>; rel="first",
    * <https://api.github.com/search/code?q=addClass+user%3Amozilla&page=13>; rel="prev"
    * ```
    */
   init {
        linkHeader?.let {header ->
            val relNextRegex = """rel="next"""".toRegex()
            val urlPatternRegex = """<(.*)>""".toRegex()
            _nextUrl = header.split(",")
                .map(String::trim)
                .filter {relNextRegex.containsMatchIn(it)}
                .map {
                    val matchResult = urlPatternRegex.find(it)
                    if (matchResult?.groupValues?.size == 2) {
                        matchResult.groupValues[1]
                    } else {
                        null
                    }
                }
                .firstOrNull()
        }

    }

    /** the url to retrieve the next content page, or null */
    val nextUrl: String? by lazy {_nextUrl}
}

class GithubReleasesClient(
        var repository: String? = null,
        var user: String? = null,
        var accessToken: String? = null,
        var apiUrl: String = DEFAULT_GITHUB_API_URL
    ) {

    private val client = OkHttpClient()

    private fun ensureParametersComplete() {
        accessToken ?: throw GithubReleaseClientException(
            "GitHub access token is missing"
        )
        user ?: throw GithubReleaseClientException(
            "GitHub user name is missing"
        )
    }

    private fun createBaseRequestBuilder() : Request.Builder {
        ensureParametersComplete()
        val credential = Credentials.basic(user!!, accessToken!!)
        return Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/vnd.github.v3.full+json")
            .addHeader("Authorization",credential)
    }

    /**
     * Fetches the list of releases
     */
    @Throws(GithubReleaseClientException::class)
    fun getReleases(): JsonArray<JsonObject> {
        var requestUrl: String? = "$apiUrl/repos/$user/$repository/releases"
        val ret = JsonArray<JsonObject>()
        while(requestUrl != null){
            val request = createBaseRequestBuilder()
                    .url(requestUrl)
                    .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw GithubReleaseClientException(response)
                }
                val pagination = Pagination(response.header("Link"))

                val releases = Parser().parse(StringBuilder(
                    response.body()?.string() ?: "[]"
                )) as JsonArray<JsonObject>
                ret.addAll(releases)
                requestUrl = pagination.nextUrl
            } catch(e: GithubReleaseClientException) {
                throw e
            } catch(e: Throwable) {
                throw GithubReleaseClientException(e)
            }
        }
        return ret
    }

    private fun Response.asJsonObject() : JsonObject {
        val body = this.body()?.string() ?:
        throw GithubReleaseClientException(
            "Unexpected response body from GitHub API"
        )
        return Parser().parse(StringBuilder(body)) as JsonObject
    }

    private fun invokeWrapped(executor: () -> JsonObject?) : JsonObject? {
        try {
            return executor()
        } catch(e: GithubReleaseClientException) {
            throw e
        } catch(e: Throwable) {
            throw GithubReleaseClientException(e)
        }
    }

    /**
     * Replies the latest release or null, if no such release exists.
     */
    @Throws(GithubReleaseClientException::class)
    fun getLatestRelease(): JsonObject? {
        val request =  createBaseRequestBuilder()
            .url("$apiUrl/repos/$user/$repository/releases/latest")
            .build()

        return invokeWrapped {
            val response = client.newCall(request).execute()
            when (response.code()) {
                HTTP_OK -> response.asJsonObject()
                404 -> null
                else -> throw GithubReleaseClientException(response)
            }
        }
    }

    /**
     * Creates a github release with the tag `tagName`.
     */
    @Throws(GithubReleaseClientException::class)
    fun createRelease(tagName: String, targetCommitish: String? = null,
            name: String? = null, body: String? = null,
            draft: Boolean = false, prerelease: Boolean = false
        ): JsonObject {

        val requestJson = JsonObject()
        requestJson["tag_name"] = tagName
        targetCommitish?.let {requestJson["target_commitish"] = it}
        // name: optional. Only include if available
        name?.let {requestJson["name"] = it}
        // body: optional. Only include if available
        body?.let {requestJson["body"] = it}
        // draft: only include, if not equal to default value, i.e. false
        if (!draft) {
          requestJson["draft"] = draft
        }
        // prerelease: only include, if not equal to default value, i.e. false
        if (!prerelease) {
          requestJson["prerelease"] = prerelease
        }

        val requestBody = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            requestJson.toJsonString()
        )

        val request = createBaseRequestBuilder()
            .post(requestBody)
            .url("$apiUrl/repos/$user/$repository/releases")
            .build()

        return invokeWrapped {
            val response = client.newCall(request).execute()
            when (response.code()) {
                HTTP_CREATED -> response.asJsonObject()
                else -> throw GithubReleaseClientException(response)
            }
        }!!
    }


    /**
     * Creates a github release with the tag `tagName`.
     */
    @Throws(GithubReleaseClientException::class)
    fun updateRelease(releaseId: Int,
                      tagName: String? = null, targetCommitish: String? = null,
                      name: String? = null, body: String? = null,
                      draft: Boolean? = null, prerelease: Boolean? = null
    ): JsonObject {

        val requestJson = JsonObject()
        // copy the attribute values to update to the request JSON
        // object
        tagName?.let {requestJson["tag_name"] = tagName}
        targetCommitish?.let {requestJson["target_commitish"] = it}
        name?.let {requestJson["name"] = it}
        body?.let {requestJson["body"] = it}
        draft?.let {requestJson["draft"] = it}
        prerelease?.let {requestJson["prerelease"] = it}

        val requestBody = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            requestJson.toJsonString()
        )

        val request = createBaseRequestBuilder()
            .patch(requestBody)
            .url("$apiUrl/repos/$user/$repository/releases/$releaseId")
            .build()

        return invokeWrapped {
            val response = client.newCall(request).execute()
            when (response.code()) {
                HTTP_OK -> response.asJsonObject()
                else -> throw GithubReleaseClientException(response)
            }
        }!!
    }

    private fun kvpair(name: String, value: String?) =
        value?.let {"$name=${URLEncoder.encode(value, "utf-8")}"}

    /**
     * Fetch the list of release assets for the release `releaseId`.
     */
    @Throws(GithubReleaseClientException::class)
    fun getReleaseAssets(releaseId: Int) : JsonArray<JsonObject> {
        try {
            var requestUrl: String? =
                "$apiUrl/repos/$user/$repository/releases/$releaseId/assets"
            val ret = JsonArray<JsonObject>()
            while (requestUrl != null) {
                val request = createBaseRequestBuilder()
                    .url(requestUrl)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw GithubReleaseClientException(response)
                }
                val pagination = Pagination(response.header("Link"))

                val releases = Parser().parse(StringBuilder(
                        response.body()?.string() ?: "[]"
                    )) as JsonArray<JsonObject>
                ret.addAll(releases)
                requestUrl = pagination.nextUrl
            }
            return ret
        } catch (e: GithubReleaseClientException) {
            throw e
        } catch (e: Throwable) {
            throw GithubReleaseClientException(e)
        }
    }

    /**
     * Upload a release asset `file` to the release `releaseId`. `name` is the
     * (optional) new name of the uploaded file. `label` is an (optional) short
     * description for the asset.
     */
    @Throws(GithubReleaseClientException::class)
    fun uploadReleaseAsset(releaseId: Int, file: File, contentType: String,
                           name: String? = null, label: String? = null)
        :JsonObject {

        @Suppress("NAME_SHADOWING")
        val name = name ?: file.name
        val mediaType =  MediaType.parse(contentType)
        val requestBody = RequestBody.create(mediaType, file)


        val url = "$apiUrl/repos/$user/$repository/releases/$releaseId" +
            "/assets?" +
            listOf(
                kvpair("name", name),
                kvpair("label", label)
            )
            .filterNotNull()
            .joinToString(separator = "&")

        val request = createBaseRequestBuilder()
            .post(requestBody)
            .url(url)
            .build()

        return invokeWrapped {
            val response = client.newCall(request).execute()
            when (response.code()) {
                HTTP_CREATED -> response.asJsonObject()
                else -> {
                    val errorMessage = response.toFormattedErrorMessage()
                    throw GithubReleaseClientException(
                        "Failed to upload file '${file.absolutePath}' as " +
                            "release asset '$name'. " +
                            "Error message: \n" + errorMessage
                    )
                }
            }
        }!!
    }

    /**
     * Deletes the release asset `assetId`
     */
    @Throws(GithubReleaseClientException::class)
    fun deleteReleaseAsset(assetId: Int) {
        val url = "$apiUrl/repos/$user/$repository/releases/assets/$assetId"
        val request = createBaseRequestBuilder()
            .delete()
            .url(url)
            .build()

        invokeWrapped {
            val response = client.newCall(request).execute()
            when (response.code()) {
                204 -> null
                else -> {
                    val errorMessage = response.toFormattedErrorMessage()
                    throw GithubReleaseClientException(
                        "Failed to delete asset '$assetId'. " +
                        "Error message: \n" + errorMessage
                    )
                }
            }
        }
    }
}
