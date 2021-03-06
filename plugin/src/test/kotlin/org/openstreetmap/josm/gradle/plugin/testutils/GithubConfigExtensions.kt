package org.openstreetmap.josm.gradle.plugin.testutils

import org.gradle.api.Project
import org.openstreetmap.josm.gradle.plugin.config.GithubConfig
import java.io.File
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.withNullability

/**
 * Create configuration for GitHub releases as you would write it in a `build.gradle` or `build.gradle.kts` file
 * @param [suppressFields] the names of those fields for which the output should be suppressed,
 *   by default this is only "releasesConfig" (that field causes problems when the file in it is not existent)
 */
fun GithubConfig.toGradleBuildscript(suppressFields: List<String> = listOf("releasesConfig")) = "github {\n  " +
  GithubConfig::class.memberProperties
    .filter { it.visibility == KVisibility.PUBLIC && it is KMutableProperty1 && !it.isConst && !suppressFields.contains(it.name) }
    .joinToString("\n  ") {
      when(it.returnType) {
        String::class.createType(), String::class.createType().withNullability(true) -> "${it.name} = ${if (it.get(this) == null) "null" else "\"${it.get(this)}\"" }"
        File::class.createType() -> "${it.name} = File(\"${(it.get(this) as File).absolutePath}\")"
        else -> TODO("type ${it.returnType} can't be printed to Gradle build script")
      }
    } + "\n}"

fun Project.buildGithubConfig(apiUrl: String, repoOwner: String, repoName: String, accessToken: String, uploadUrl: String = apiUrl): GithubConfig {
    extensions.extraProperties.set(GithubConfig.PROPERTY_ACCESS_TOKEN, accessToken)
    return GithubConfig(this).apply {
      this.repositoryOwner = repoOwner
      this.repositoryName = repoName
      this.apiUrl = apiUrl
      this.uploadUrl = uploadUrl
    }
}
