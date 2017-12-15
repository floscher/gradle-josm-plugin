package org.openstreetmap.josm.gradle.plugin.setup;

import org.gradle.api.Project;

public abstract class AbstractSetup {
  protected final Project pro;
  public AbstractSetup(final Project project) {
    this.pro = project;
  }
  abstract void setup();
}
