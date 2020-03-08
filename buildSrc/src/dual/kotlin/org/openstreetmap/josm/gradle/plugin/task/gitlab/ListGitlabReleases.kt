package org.openstreetmap.josm.gradle.plugin.task.gitlab

import kotlinx.serialization.KSerializer
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.JsonConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.openstreetmap.josm.gradle.plugin.api.github.GithubRelease
import org.openstreetmap.josm.gradle.plugin.api.gitlab.GitlabRelease
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

open class ListGitlabReleases(): DefaultTask() {
  @UnstableDefault
  @TaskAction
  fun action() {
    val list = parsePaginatedApiList(URL("https://gitlab.com/api/v4/projects/JOSM%2Fplugin%2Fwikipedia/releases"), GitlabRelease.serializer(GitlabRelease.Assets.Link.Existing.serializer()))
    list.forEach {
      println("${it.name} ${it.tagName}")
      it.assets.links.forEach { println("  ${it.name} ${it.url}") }
    }
    val list2 = parsePaginatedApiList(URL("https://api.github.com/repos/JOSM/Mapillary/releases?per_page=100"), GithubRelease.serializer())
    list2.forEach {
      println("${it.name} ${it.tagName}")
      it.assets.forEach { println("  ${it.name} ${it.url}") }
    }
  }

  val linkHeaderRegex = Regex("""\s*<([^>]+)>\s*;\s*rel\s*=\s*"([a-z]+)"\s*""")

  @UnstableDefault
  @Throws(IOException::class)
  fun <T> parsePaginatedApiList(url: URL, serializer: KSerializer<T>, depth: Int = 0): List<T> {
    require(depth < 100) { "Max. recursion depth for querying paginated lists from APIs is currently 100!" }
    println(url.toString())
    val connection = (url.openConnection() as HttpsURLConnection)
    println(connection.responseCode)
    if (connection.responseCode != 200) {
      throw IOException("Server returned ${connection.responseCode} for URL $url:\n${connection.errorStream.bufferedReader().readText()}")
    } else {
      return Json(JsonConfiguration.Nonstrict).parse(serializer.list, connection.inputStream.bufferedReader().readText()).plus(
      connection.headerFields.get("Link")
        ?.flatMap { it.split(',').mapNotNull { linkHeaderRegex.matchEntire(it) } }
        ?.firstOrNull { it.groupValues[2] == "next" }
        ?.let { parsePaginatedApiList(URL(it.groupValues[1]), serializer, depth.inc()) }
        ?: listOf()
      )
    }
  }
}
