package org.openstreetmap.josm.gradle.plugin.util

import java.net.URL

object Urls {
  object MainJosmWebsite {
    val BASE = URL("https://josm.openstreetmap.de")
    val DOWNLOADS = URL("$BASE/download")
    val VERSION_NUMBER_LATEST = URL("$BASE/latest")
    val VERSION_NUMBER_TESTED = URL("$BASE/tested")
    val NEXUS_REPO_RELEASES = URL("$BASE/nexus/content/repositories/releases")
    val NEXUS_REPO_SNAPSHOTS = URL("$BASE/nexus/content/repositories/snapshots")

    /** https://josm.openstreetmap.de/plugin */
    val PATH_PLUGIN_LIST = "plugin"
    /** https://josm.openstreetmap.de/pluginicons */
    val PATH_PLUGIN_LIST_WITH_ICONS = "pluginicons"
  }
  object Github {
    /** the default API URL for the GitHub API */
    val API = URL("https://api.github.com")
    /** the default upload URL to upload a release asset */
    val UPLOADS = URL("https://uploads.github.com")
  }

  /** The gitlab.com Maven repository containing all packages released by the `JOSM` group on gitlab.com */
  val GITLAB_JOSM_PLUGINS_REPO = URL("https://gitlab.com/api/v4/groups/JOSM/-/packages/maven")
  /** The directory in the JOSM SVN where the latest releases of JOSM plugins are stored */
  val JOSM_PLUGIN_DIST = URL("https://svn.openstreetmap.org/applications/editors/josm/dist")
}
