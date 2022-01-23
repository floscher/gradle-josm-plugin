package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.openstreetmap.josm.gradle.plugin.util.Urls
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Task for showing the current `latest` and `tested` JOSM versions
 */
open class ListJosmVersions : DefaultTask() {

  companion object {
    const val ERROR_CODE_CONNECTION = -1
    const val ERROR_CODE_WRONG_FORMAT = -2
  }

  /**
   * The encoding of the output file (default is UTF-8).
   * @since 0.8.0
   */
  @Internal // can't be used as @Input, because Charset is not serializable, but update-checking is turned off anyway
  public val encoding: Property<Charset> = project.objects.property(Charset::class.java)
    .convention(StandardCharsets.UTF_8)

  /**
   * The file to which the version numbers are written.
   * @since 0.8.0
   */
  @OutputFile
  public val outputFile: RegularFileProperty = project.objects.fileProperty()
    .convention { project.buildDir.resolve("josmVersionList.properties") }

  init {
    group = "JOSM"
    description = "Shows the current 'latest' and 'tested' JOSM versions"
    outputs.upToDateWhen { false } // always rerun
  }

  @TaskAction
  fun action() {
    """
      # Available JOSM versions ($ERROR_CODE_CONNECTION means connection error, $ERROR_CODE_WRONG_FORMAT means something that was not a positive integer was read)
      latest=${ readNumericVersion(Urls.MainJosmWebsite.VERSION_NUMBER_LATEST) }
      tested=${ readNumericVersion(Urls.MainJosmWebsite.VERSION_NUMBER_TESTED) }
      # (There might be even later versions available, 'latest' refers to the latest nightly build)
    """
      .trimIndent()
      .let {
        logger.lifecycle(it)
        outputFile.asFile.get().writeText(it, encoding.get())
      }
  }

  private fun readNumericVersion(url: URL): Int = try {
    url.readText().toIntOrNull()?.takeIf { it > 0 } ?: ERROR_CODE_WRONG_FORMAT
  } catch (e: IOException) {
    ERROR_CODE_CONNECTION
  }
}

