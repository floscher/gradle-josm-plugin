package org.openstreetmap.josm.gradle.plugin.io

import java.net.URL

/**
 * Encapsulates the plugin metadata that can be found for each plugin in the plugin list.
 * @property pluginName the name of the JOSM plugin
 * @property downloadUrl the URL from where the plugin can be downloaded
 * @property manifestAtts the attributes of the `MANIFEST.MF` file of the plugin
 */
data class PluginInfo(val pluginName: String, val downloadUrl: URL, val manifestAtts: Map<String, String>) {
  companion object {
    fun build(pluginName: String?, downloadUrl: URL?, manifestAtts: Map<String, String>) = pluginName?.let { name ->
      downloadUrl?.let { url ->
        PluginInfo(name, url, manifestAtts.toMap())
      }
    }
  }
}
