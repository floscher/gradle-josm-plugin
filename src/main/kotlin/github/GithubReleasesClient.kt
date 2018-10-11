package org.openstreetmap.josm.gradle.plugin.github

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.openstreetmap.josm.gradle.plugin.config.GithubConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URLEncoder

@Throws(GithubReleaseException::class)
private fun Response.toFormattedErrorMessage() : String {
  val body = this.body()?.string()
    ?: throw GithubReleaseException(
      "Unexpected error response body from GitHub API"
    )
  return try {
    Parser().parse(StringBuilder(body)) as JsonObject
  } catch(t: Throwable) {
    null
  }?.toJsonString(prettyPrint = true) ?: body
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
      _nextUrl = header.split(",").asSequence()
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
  val repository: String,
  val user: String,
  val accessToken: String,
  val apiUrl: String
) {
  constructor(github: GithubConfig, apiUrl: String): this(
    github.repositoryName,
    github.repositoryOwner,
    github.accessToken,
    apiUrl
  )

  private val client = OkHttpClient()

  private fun createBaseRequestBuilder() : Request.Builder {
    val credential = Credentials.basic(user, accessToken)
    return Request.Builder()
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/vnd.github.v3.full+json")
      .addHeader("Authorization",credential)
  }

  /**
   * Fetches the list of releases
   */
  @Throws(GithubReleaseException::class)
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
          throw GithubReleaseException(response)
        }
        val pagination = Pagination(response.header("Link"))

        val releases = Parser().parse(StringBuilder(
          response.body()?.string() ?: "[]"
        )) as JsonArray<JsonObject>
        ret.addAll(releases)
        requestUrl = pagination.nextUrl
      } catch(e: GithubReleaseException) {
        throw e
      } catch(e: Throwable) {
        throw GithubReleaseException(e)
      }
    }
    return ret
  }

  private fun Response.asJsonObject() : JsonObject {
    val body = this.body()?.string() ?:
    throw GithubReleaseException(
      "Unexpected response body from GitHub API"
    )
    return Parser().parse(StringBuilder(body)) as JsonObject
  }

  private fun invokeWrapped(executor: () -> JsonObject?) : JsonObject? {
    try {
      return executor()
    } catch(e: GithubReleaseException) {
      throw e
    } catch(e: Throwable) {
      throw GithubReleaseException(e)
    }
  }

  /**
   * Replies the latest release or null, if no such release exists.
   */
  @Throws(GithubReleaseException::class)
  fun getLatestRelease(): JsonObject? {
    val request =  createBaseRequestBuilder()
      .url("$apiUrl/repos/$user/$repository/releases/latest")
      .build()

    return invokeWrapped {
      val response = client.newCall(request).execute()
      when (response.code()) {
        HttpURLConnection.HTTP_OK -> response.asJsonObject()
        404 -> null
        else -> throw GithubReleaseException(response)
      }
    }
  }

  /**
   * Creates a github release with the tag `tagName`.
   *
   * See [API documentation](https://developer.github.com/v3/repos/releases/#create-a-release)
   * @param [tagName] The name of the tag
   * @param [targetCommitish] the commitish value that determines where the Git tag is created from
   * @param [name] the title displayed together with the body
   * @param [body] a description of the release
   * @param [draft] if set to true, the release is only a drafting stage
   * @param [prerelease] if set to true, the release is only in alpha or beta stage, not a final release
   * @return the JSON object that should be sent to the GitHub API
   */
  @Throws(GithubReleaseException::class)
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
    if (draft) {
      requestJson["draft"] = draft
    }
    if (prerelease) {
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
        HttpURLConnection.HTTP_CREATED -> response.asJsonObject()
        else -> throw GithubReleaseException(response)
      }
    }!!
  }


  /**
   * Creates a github release with the tag `tagName`.
   *
   * See [GitHub API documentation](https://developer.github.com/v3/repos/releases/#edit-a-release).
   */
  @Throws(GithubReleaseException::class)
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
        HttpURLConnection.HTTP_OK -> response.asJsonObject()
        else -> throw GithubReleaseException(response)
      }
    }!!
  }

  private fun kvpair(name: String, value: String?) =
    value?.let {"$name=${URLEncoder.encode(value, "utf-8")}"}

  /**
   * Fetch the list of release assets for the release `releaseId`.
   */
  @Throws(GithubReleaseException::class)
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
          throw GithubReleaseException(response)
        }
        val pagination = Pagination(response.header("Link"))

        val releases = Parser().parse(StringBuilder(
          response.body()?.string() ?: "[]"
        )) as JsonArray<JsonObject>
        ret.addAll(releases)
        requestUrl = pagination.nextUrl
      }
      return ret
    } catch (e: GithubReleaseException) {
      throw e
    } catch (e: Throwable) {
      throw GithubReleaseException(e)
    }
  }

  /**
   * Upload a release asset `file` to the release `releaseId`. `name` is the
   * (optional) new name of the uploaded file. `label` is an (optional) short
   * description for the asset.
   */
  @Throws(GithubReleaseException::class)
  fun uploadReleaseAsset(releaseId: Int, file: File, contentType: String,
                         name: String? = null, label: String? = null)
    : JsonObject {

    @Suppress("NAME_SHADOWING")
    val name = name ?: file.name
    val mediaType =  MediaType.parse(contentType)
    val requestBody = RequestBody.create(mediaType, file)


    val url = "$apiUrl/repos/$user/$repository/releases/$releaseId" +
      "/assets?" +
      listOfNotNull(
        kvpair("name", name),
        kvpair("label", label)
      ).joinToString("&")

    val request = createBaseRequestBuilder()
      .post(requestBody)
      .url(url)
      .build()

    return invokeWrapped {
      val response = client.newCall(request).execute()
      when (response.code()) {
        HttpURLConnection.HTTP_CREATED -> response.asJsonObject()
        else -> {
          val errorMessage = response.toFormattedErrorMessage()
          throw GithubReleaseException(
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
  @Throws(GithubReleaseException::class)
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
          throw GithubReleaseException(
            "Failed to delete asset '$assetId'. " +
              "Error message: \n" + errorMessage
          )
        }
      }
    }
  }
}
