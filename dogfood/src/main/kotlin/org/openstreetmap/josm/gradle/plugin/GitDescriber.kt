package org.openstreetmap.josm.gradle.plugin

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.io.IOException

/**
 * Uses jGit to describe the current HEAD of a git repository
 * @param workTree the directory where the work tree of the git repository is
 *   (typically the parent directory of the `.git` directory)
 */
public class GitDescriber(private val workTree: File) : Describer {
  private val git = Git(FileRepositoryBuilder().findGitDir(workTree).readEnvironment().build())

  /**
   * Replicates the `git describe` command. Never null, either returns a [String] or throws an exception
   * @param markSnapshots iff true, the string [Describer.SNAPSHOT_SUFFIX] is appended when there are modified files
   *   in the work tree compared to the HEAD
   * @param trimLeading iff true, the leading character `v` is removed from the version number (if present)
   * @return a string that describes the current HEAD of the git repository
   */
  @Throws(IOException::class, GitAPIException::class)
  override fun describe(markSnapshots: Boolean, trimLeading: Boolean): String =
    // result of `git describe`, if not applicable the commit hash
    (git.describe().call() ?: commitHash())
      .let {
        if (trimLeading && it.length >= 2 && it[0] == 'v') {
          it.substring(1)
        } else it
      }
      .let {
        if (markSnapshots && git.status().call().hasUncommittedChanges()) {
          it + Describer.SNAPSHOT_SUFFIX
        } else it
      }

  /**
   * @return the abbreviated but unique commit hash of the current HEAD
   */
  public fun commitHash(): String =
    git.repository.newObjectReader().abbreviate(
      git.log().setMaxCount(1).call().first().toObjectId()
    ).name()
}
