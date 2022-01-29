package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import javax.inject.Inject

/**
 * Task that simply copies the archive file produced by the given [archiverTask]
 */
public open class RenameArchiveFile @Inject constructor(
  @get:InputFiles
  public val archiverTask: TaskProvider<AbstractArchiveTask>,
  @get:Internal
  public val targetDir: Provider<out Directory>,
  @get:Input
  public val fileBaseName: Provider<out String>
): DefaultTask() {


  private val fileName: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
    "${fileBaseName.get()}.${archiverTask.get().archiveExtension.get()}"
  }
  @get:OutputFile
  public val targetFile: Provider<RegularFile> = targetDir.map { it.file(fileName) }

  @TaskAction
  public fun copy() {
    project.sync {
      it.from(archiverTask.map { it.archiveFile })
      it.into(targetDir)
      it.rename { fileName }
      it.duplicatesStrategy = DuplicatesStrategy.FAIL
    }
    logger.lifecycle(
      """
        Copied file
          from ${archiverTask.get().archiveFile.get()}
          into ${targetDir.get().asFile.canonicalPath}/$fileName
      """.trimIndent()
    )
  }
}
