package org.openstreetmap.josm.gradle.plugin.ghreleases

import java.io.File


import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.RequestBody

import com.beust.klaxon.Json
import com.beust.klaxon.Parser
import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonArray

// the default API URL for the Github API
const val DEFAULT_GITHUB_API_URL = "https://api.github.com"

//TODO: add a factory class to create a GithubReleaseClient for a given
// gradle project or gradle task. It should initialize the parameters
// for the repos, the user and the accessToken from environment variables
// and/or gradle properties


data class Release (
    @Json(name = "id")
    val id: Int,
    
    @Json(name = "url")
    val url: String,

    @Json(name = "assets_url")
    val assetsUrl: String
)

class GithubReleaseClientException(override var message: String, override var cause: Throwable?) : Exception(message, cause) {
  constructor(message: String) : this(message, null) {
  }
}

/**
 * Information about related content pages in a sequence of content
 * pages retured by an API method.
 *
 * Limited in functionality. Only considers paging links of type "next"
*/
class Pagination() {

    private var _next: String? = null

    /**
    *  Parses the relation URLs in `linkHeader` (if not null). Pass in the
    *  header value only, without the header name `Link:`
    *
    * Sample of a link header (including the Header name) replyed by the Github
    * API
    * ```
    * Link: <https://api.github.com/search/code?q=addClass+user%3Amozilla&page=15>; rel="next",
    * <https://api.github.com/search/code?q=addClass+user%3Amozilla&page=34>; rel="last",
    * <https://api.github.com/search/code?q=addClass+user%3Amozilla&page=1>; rel="first",
    * <https://api.github.com/search/code?q=addClass+user%3Amozilla&page=13>; rel="prev"
    * ```
    */
    constructor(linkHeader: String?) : this() {
        if (linkHeader == null) {
            return
        }
        val tokens = linkHeader.split(",")
        val relNextRegex = """rel=\"next\"""".toRegex()
        val urlPatternRegex = """<(.*)>""".toRegex()
        this._next = tokens
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

    /** true, if there is an url to the next content page */
    var hasNext: Boolean = false
        get() = this._next != null
    /** the url to retrieve the next content pages, or null */
    var nextUrl: String? = null
        get() = this._next
}

class GithubReleasesClient(
        var repository: String? = null,
        var user: String? = null,
        var accessToken: String? = null,
        var apiUrl: String = DEFAULT_GITHUB_API_URL
    ) {

    private val client = OkHttpClient()

    private fun ensureParametersComplete() {
        if (accessToken == null) {
            //TODO more specific exception
            throw Exception("Requires a valid github access token")
        }
        if (user == null) {
            //TODO more specific exception
            throw Exception("Requires defined user")
        }        
    }

    private fun createBaseRequestBuilder() : Request.Builder {
        ensureParametersComplete()
        val credential = Credentials.basic(user!!, accessToken!!);
        return Request.Builder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/vnd.github.v3.full+json")
                .addHeader("Authorization",credential)
    }
 
    fun getReleases(): JsonArray<JsonObject> {
        var requestUrl: String? = "${apiUrl}/repos/${user}/${repository}/releases"
        var ret = JsonArray<JsonObject>()
        do {
            val request =  createBaseRequestBuilder()
                    .url(requestUrl!!)
                    .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful()) {
                    //TODO more specific exception
                    throw  Exception("Unexpected response: " + response)
                }
                val pagination = Pagination(response.header("Link"))

                val parser = Parser()
                val releases = parser.parse(StringBuilder(
                    response.body()?.string() ?: "[]"
                )) as JsonArray<JsonObject>
                ret.addAll(releases)
                requestUrl = pagination.nextUrl
            } catch(e: Throwable) {
                // TODO wrap exception?
                throw e;
            }
        } while(requestUrl != null)
        return ret
    }

  /**
   * Replies the latest release or null, if no such release exists.
   */
  @Throws(GithubReleaseClientException::class)
  fun getLatestRelease(): JsonObject? {
        val request =  createBaseRequestBuilder()
                .url("${apiUrl}/repos/${user}/${repository}/releases/latest")
                .build()

        try {
          val response = client.newCall(request).execute()
          when (response.code()) {
            in 200..299 -> {
              val parser = Parser()
              return parser.parse(StringBuilder(
                response.body()?.string() ?: "null"
              )) as JsonObject
            }
            404 -> return null
            else -> throw GithubReleaseClientException("Unexpected response with code ${response.code()}. "
                + "Response body: ${response.body()?.string()}")

          }
        } catch(e: GithubReleaseClientException) {
          throw e
        } catch(e: Exception) {
            throw GithubReleaseClientException(e.message ?: "", e)
        }
    }

    fun createRelease(name: String, body: String): JsonObject {
        //TODO use a more stable way to create the JSON body 
        val jsonBody = """{
                "name": "${name}",
                "body": "${body}",
                "tag_name": "v0.0.1",
                "draft": true
            }"""
        val jsonMediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(jsonMediaType, jsonBody)

        val request = createBaseRequestBuilder()
            .post(requestBody)
            .url("${apiUrl}/repos/${user}/${repository}/releases")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful()) {
                println(response.body()?.string() ?: "null")
                //TODO more specific exception
                throw  Exception("Unexpected response: " + response)
            }
            val parser = Parser()
            return parser.parse(StringBuilder(
                response.body()?.string() ?: "null"
            )) as JsonObject
        } catch(e: Exception) {
            // TODO wrap exception?
            throw e;
        }
    }

    fun uploadAsset(releaseId: Int, jar: File) : JsonObject {
        val fileName = jar.getName()
        val mediaType =  MediaType.parse("application/java-archive")
        val requestBody = RequestBody.create(mediaType, jar)
        val request = createBaseRequestBuilder()
            .post(requestBody)
            .url("${apiUrl}/repos/${user}/${repository}/releases/${releaseId}/assets?${fileName}")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful()) {
                println(response.body()?.string() ?: "null")
                //TODO more specific exception
                throw  Exception("Unexpected response: " + response)
            }
            val parser = Parser()
            return parser.parse(StringBuilder(
                response.body()?.string() ?: "null"
            )) as JsonObject
        } catch(e: Exception) {
            // TODO wrap exception?
            throw e;
        }
    }
}
