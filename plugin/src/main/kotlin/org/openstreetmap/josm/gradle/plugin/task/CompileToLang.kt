package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet
import org.openstreetmap.josm.gradle.plugin.i18n.io.GETTEXT_HEADER_MSGID
import org.openstreetmap.josm.gradle.plugin.i18n.io.I18nFileDecoder
import org.openstreetmap.josm.gradle.plugin.i18n.io.LangFileEncoder
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgId
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgStr
import org.openstreetmap.josm.gradle.plugin.i18n.util.formatAsProgressBar
import org.openstreetmap.josm.gradle.plugin.util.josm
import java.io.File

/**
 * @param sourceSet the source set containing the i18n source files
 * @param extractSources a function that extracts the relevant [SourceDirectorySet] from the [I18nSourceSet]
 * @param fileExtension the file extension of the i18n source files (`po`, `mo` or `lang`)
 * @param decoder the decoder that decodes the translations from the source files
 */
public abstract class CompileToLang(
  @Internal val sourceSet: I18nSourceSet,
  @Internal val extractSources: (I18nSourceSet).() -> SourceDirectorySet,
  @Internal val fileExtension: String
): DefaultTask() {

  @get:Internal
  abstract val decoder: I18nFileDecoder

  @get:Internal
  public lateinit var translations: List<Pair<String, Map<MsgId, MsgStr>>>

  open fun filterIsExcludedBaseFile(file: File): Boolean = false

  @Input
  val baseLanguage: Provider<String> = project.provider { project.extensions.josm.i18n.mainLanguage }

  @InputFiles
  val inputFiles: Provider<Set<File>> = project.provider { extractSources(sourceSet).files }

  @OutputDirectory
  val outputDirectory: Provider<File> = project.provider { project.buildDir.resolve("i18n/${sourceSet.name}/$fileExtension") }

  @Internal
  override fun getDescription(): String = "Compile the *.$fileExtension files of source set `${sourceSet.name}` to the *.lang format used by JOSM (${extractSources(sourceSet).files.size} files available)"
  override fun setDescription(description: String?) = throw IllegalArgumentException("Changing description of a ${this::class.simpleName} task is not allowed!")

  @Internal
  override fun getGroup(): String = "JOSM-i18n"
  override fun setGroup(group: String?) = throw IllegalArgumentException("Changing group of a ${this::class.simpleName} task is not allowed!")

  @TaskAction
  fun action() {
    val baseLanguage = baseLanguage.get()
    val inputFiles = inputFiles.get()

    // Delete with safeguard, only delete from build directory
    outputDirectory.orNull?.takeIf { it.exists() && it.hasParent(project.buildDir) }?.deleteRecursively()

    val outputDirectory = outputDirectory.get().resolve("data")
    outputDirectory.mkdirs()

    if (inputFiles.isEmpty()) {
      logger.lifecycle(
        "No *.$fileExtension files found to be compiled for source set ${sourceSet.name} in:\n" +
          sourceSet.mo.srcDirs.joinToString("\n") { "  " + it.absolutePath }
      )
      translations = emptyList()
    } else {
      require(inputFiles.map { it.nameWithoutExtension.lowercase() }.let { it.size == it.distinct().size }) {
        "There are duplicate locales: ${ inputFiles.map { it.nameWithoutExtension }.sortedBy { it.lowercase() }.joinToString() }"
      }

      require(inputFiles.filterNot(this::filterIsExcludedBaseFile).none { it.nameWithoutExtension.lowercase() == baseLanguage }) {
        "Do not provide a file `$baseLanguage.$fileExtension` for the base language! The strings of the base language are automatically inferred from the other languages."
      }
      compile(baseLanguage, inputFiles.filterNot(this::filterIsExcludedBaseFile).toSet(), outputDirectory)
    }
  }

  @OptIn(ExperimentalUnsignedTypes::class)
  private fun compile(baseLanguage: String, inputFiles: Set<File>, outputDirectory: File) {
    logger.lifecycle(extractSources(sourceSet).srcDirs.joinToString("\n") { "from: ${it.absolutePath}" })
    logger.lifecycle("into: $outputDirectory")

    this.translations = inputFiles.map {
      it.nameWithoutExtension to decoder.decodeToTranslations(it.readBytes())
    }.also {
      logger.lifecycle("locales: ${it.map { it.first }.sorted().joinToString()} (+ $baseLanguage)")
    }

    val keys = translations.flatMap { it.second.keys }.distinct()
    val totalNumStrings = keys.minus(GETTEXT_HEADER_MSGID).size
    val encoder = LangFileEncoder(keys)
    outputDirectory.resolve("$baseLanguage.lang").apply {
      writeBytes(encoder.encodeToBaseLanguageByteArray())
      logger.lifecycle("$totalNumStrings strings for base language $nameWithoutExtension")
    }

    translations.sortedBy { it.first }.sortedByDescending { it.second.values.size }.forEach { (language, translation) ->
      outputDirectory.resolve("${language}.lang").apply {
        writeBytes(encoder.encodeToByteArray(translation))
        translation.filter { it.key != GETTEXT_HEADER_MSGID }.mapNotNull { it.value }.size.let { numCompleted ->
          logger.lifecycle("${
            formatAsProgressBar(numCompleted.toUInt(), totalNumStrings.toUInt())
          } (${
            numCompleted.toString().padStart(totalNumStrings.toString().length)
          } of $totalNumStrings strings) for $nameWithoutExtension")
        }
      }
    }
  }

  private fun File.hasParent(target: File): Boolean = when {
    parentFile.absolutePath == target.absolutePath -> true
    parentFile != null -> parentFile.hasParent(target)
    else -> false
  }
}
