package org.openstreetmap.josm.plugins.myawesomeplugin;

import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.download.DownloadSelection;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

import java.util.List;

public class MyAwesomePlugin extends Plugin {
  public MyAwesomePlugin(PluginInformation info) {
    super(info);
    // Initialize the plugin here
  }

  @Override
  public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
    super.mapFrameInitialized(oldFrame, newFrame);
    // Handle changes to the active MapFrame
  }

  @Override
  public PluginInformation getPluginInformation() {
    // Supply an editor for the plugin preferences, if needed.
    return super.getPluginInformation();
  }

  @Override
  public void addDownloadSelection(List<DownloadSelection> list) {
    super.addDownloadSelection(list);
    // You can supply your own download method
  }
}
