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
class GitDescriber(val workTree: File) {
  private val git = Git(FileRepositoryBuilder().setWorkTree(workTree).readEnvironment().findGitDir().build())

  /**
   * Replicates the `git describe` command. Never null, either returns a [String] or throws an exception
   * @param dirty if true, the string "-dirty" is appended when there are modified files in the work tree compared to the HEAD
   * @return a string that describes the current HEAD of the git repository
   */
  @Throws(IOException::class, GitAPIException::class)
  fun describe(dirty: Boolean = true): String {
    val description = git.describe().call()
    return if (description == null) {
      // return abbreviated hash
      commitHash()
    } else if (dirty && git.status().call().hasUncommittedChanges()) {
      "$description-dirty"
    } else {
      description
    }
  }

  /**
   * Returns the abbreviated but unique commit hash of the current HEAD
   */
  fun commitHash(): String {
    return git.repository.newObjectReader().abbreviate(
      git.log().setMaxCount(1).call().first().toObjectId()
    ).name()
  }
}
