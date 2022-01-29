package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.Transformer
import org.gradle.api.file.Directory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import javax.inject.Inject

/**
 * Task that simply copies the archive file produced by the given [archiverTask]
 */
public open class RenameArchiveFile @Inject constructor(
  private val archiverTask: TaskProvider<out AbstractArchiveTask>,
  private val targetDir: Provider<out Directory>,
  fileBaseName: Provider<out String>
): Sync() {

  private val fileName by lazy(LazyThreadSafetyMode.PUBLICATION) {
    "${fileBaseName.get()}.${archiverTask.get().archiveExtension.get()}"
  }

  init {
    from(archiverTask.map { it.archiveFile })
    into(targetDir)
    rename { fileName }
    duplicatesStrategy = DuplicatesStrategy.FAIL
  }

  final override fun from(vararg sourcePaths: Any?): AbstractCopyTask = super.from(*sourcePaths)
  final override fun into(destDir: Any): AbstractCopyTask = super.into(destDir)
  final override fun rename(renamer: Transformer<String, String>): AbstractCopyTask = super.rename(renamer)

  @TaskAction
  override fun copy() {
    super.copy()
    logger.lifecycle(
      "Distribution {} (version {}) has been written into {}",
      fileName,
      project.version,
      targetDir.get().asFile.canonicalPath
    )
  }
}
