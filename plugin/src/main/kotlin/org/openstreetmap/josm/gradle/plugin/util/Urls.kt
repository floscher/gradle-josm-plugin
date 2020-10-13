package org.openstreetmap.josm.gradle.plugin.util

import java.net.URL

/**
 * Central place for URLs, so they can be reused easier.
 */
object Urls {
  /**
   * URLs for the JOSM website [https://josm.openstreetmap.de]
   */
  object MainJosmWebsite {
    /**
     * The Base URL of the JOSM website
     */
    val BASE = URL("https://josm.openstreetmap.de")
    /**
     * The URL of the downloads page of the JOSM website
     */
    val DOWNLOADS = URL("$BASE/download")
    /**
     * The URL that returns the version number of the `latest` JOSM version in plain text
     */
    val VERSION_NUMBER_LATEST = URL("$BASE/latest")
    /**
     * The URL that returns the version number of the current `latest` JOSM version in plain text
     */
    val VERSION_NUMBER_TESTED = URL("$BASE/tested")
    /**
     * The URL of the JOSM  Nexus repo for releases
     */
    val NEXUS_REPO_RELEASES = URL("$BASE/nexus/content/repositories/releases")
    /**
     * The URL of the JOSM  Nexus repo for snapshots
     */
    val NEXUS_REPO_SNAPSHOTS = URL("$BASE/nexus/content/repositories/snapshots")
    /**
     * The directory in the JOSM SVN where the latest releases of JOSM plugins are stored
     */
    val PLUGIN_DIST_DIR = URL("$BASE/osmsvn/applications/editors/josm/dist")

    /**
     * The path portion of the URL https://josm.openstreetmap.de/plugin where a list of JOSM plugins can be downloaded.
     */
    val PATH_PLUGIN_LIST = "plugin"
    /**
     * The path portion of the URL https://josm.openstreetmap.de/pluginicons where a list of JOSM plugins including
     * their Base64 encoded icons can be downloaded.
     */
    val PATH_PLUGIN_LIST_WITH_ICONS = "pluginicons"
  }

  /**
   * URLs for [https://github.com]
   */
  object Github {
    /**
     * the default API URL for the GitHub API
     */
    val API = URL("https://api.github.com")
    /**
     * the default upload URL to upload a release asset
     */
    val UPLOADS = URL("https://uploads.github.com")
  }

  /**
   * The gitlab.com Maven repository containing all packages released by the `JOSM` group on gitlab.com
   */
  val GITLAB_JOSM_PLUGINS_REPO = URL("https://gitlab.com/api/v4/groups/JOSM/-/packages/maven")
}
