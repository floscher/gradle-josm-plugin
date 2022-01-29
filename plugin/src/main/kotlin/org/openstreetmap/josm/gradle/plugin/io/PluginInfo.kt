package org.openstreetmap.josm.gradle.plugin.io

import java.io.Serializable
import java.net.URI

/**
 * Encapsulates the plugin metadata that can be found for each plugin in the plugin list.
 * @property pluginName the name of the JOSM plugin
 * @property downloadUri the URI from where the plugin can be downloaded
 * @property manifestAtts the attributes of the `MANIFEST.MF` file of the plugin
 */
public data class PluginInfo(val pluginName: String, val downloadUri: URI, val manifestAtts: Map<String, String>): Serializable {
  public companion object {
    public fun build(pluginName: String?, downloadUri: URI?, manifestAtts: Map<String, String>): PluginInfo? = pluginName?.let { name ->
      downloadUri?.let { uri ->
        PluginInfo(name, uri, manifestAtts.toMap())
      }
    }
  }
}
