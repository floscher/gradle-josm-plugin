package org.openstreetmap.josm.gradle.plugin

import java.io.IOException

/**
 * A describer can determine the version number of the current state of a directory.
 * There is a [GitDescriber] and an [SvnDescriber], which use the information
 * from the respective version control repository.
 */
public interface Describer {
  public companion object {
    public const val SNAPSHOT_SUFFIX: String = "-SNAPSHOT";
  }

  /**
   * @param [markSnapshots] iff set to true, the return value gets appended with [SNAPSHOT_SUFFIX] if there are changes
   *   to the repository that are not recorded in the current revision of the VCS.
   * @param [trimLeading] iff set to true, the leading character (`r` for SVN, `v` for git) is not included in the returned version number
   * @return an identifier of the current version of the repository
   */
  @Throws(IOException::class)
  public fun describe(markSnapshots: Boolean = true, trimLeading: Boolean = true): String
}
