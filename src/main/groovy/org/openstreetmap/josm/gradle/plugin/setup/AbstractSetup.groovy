package org.openstreetmap.josm.gradle.plugin.setup;

import org.gradle.api.Project;
import org.openstreetmap.josm.gradle.plugin.JosmPlugin;

@groovy.transform.CompileStatic
public abstract class AbstractSetup {
  protected Project pro = JosmPlugin.getCurrentProject();
  abstract void setup();
}
