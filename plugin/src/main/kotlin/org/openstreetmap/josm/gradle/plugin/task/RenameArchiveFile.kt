package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.file.Directory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Zip
import java.util.jar.JarInputStream
import javax.inject.Inject

/**
 * Task that simply copies the archive file produced by the given [archiverTask]
 */
public open class RenameArchiveFile @Inject constructor(
  @get:InputFiles
  public val archiverTask: TaskProvider<out AbstractArchiveTask>,
  @get:InputFiles
  public val manifestTask: Provider<GenerateJarManifest>,
  @get:Internal
  public val targetDir: Provider<Directory>,
  @get:Input
  public val fileBaseName: Provider<String>
): Zip() {
  // TODO: Add option to append file hash to manifest version number (useful for local update site, so JOSM detects changed plugin)

  public companion object {
    /** Typesafe constructor for [RenameArchiveFile] task */
    public fun <R: RenameArchiveFile> TaskContainer.register(
      name: String,
      archiverTask: TaskProvider<out AbstractArchiveTask>,
      manifestTask: Provider<GenerateJarManifest>,
      targetDir: Provider<Directory>,
      fileBaseName: Provider<String>
    ): TaskProvider<RenameArchiveFile> =
      register(name, RenameArchiveFile::class.java, archiverTask, manifestTask, targetDir, fileBaseName)
  }

  init {
    archiveFileName.let {
      it.set(fileBaseName.map { fileBaseName ->  "$fileBaseName.${archiverTask.get().archiveExtension.get()}" })
      it.finalizeValue()
    }
  }

  init {
    from(
      /* It is very important that the MANIFEST.MF file comes first!
       * Otherwise it can't be read by a [JarInputStream]. See https://github.com/floscher/gradle-josm-plugin/pull/15
       */
      manifestTask.map { project.fileTree(it.outputDirectory) { it.include(GenerateJarManifest.MANIFEST_PATH) } },
      archiverTask.map { project.zipTree(it.archiveFile).matching { it.exclude(GenerateJarManifest.MANIFEST_PATH) } }
    )
    destinationDirectory.set(targetDir)
    duplicatesStrategy = DuplicatesStrategy.FAIL
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    includeEmptyDirs = false
  }

  @TaskAction
  public override fun copy() {
    super.copy()
    logger.lifecycle(
      """
        Copied file
          from ${archiverTask.get().archiveFile.get().asFile.canonicalPath}
          into ${archiveFile.get().asFile.canonicalPath}

        The file `${GenerateJarManifest.MANIFEST_PATH}` within the archive was swapped for ${
          manifestTask.get().outputFile.get().asFile.canonicalPath
        }
      """.trimIndent()
    )
  }
}
