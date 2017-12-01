package org.openstreetmap.josm.gradle.plugin.setup

import org.gradle.api.Project
import org.openstreetmap.josm.gradle.plugin.JosmPlugin

protected abstract class AbstractSetup {
  protected Project pro = JosmPlugin.currentProject
  abstract void setup();
}
