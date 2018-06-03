package org.openstreetmap.josm.gradle.plugin.setup;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask;

public class GithubReleasesTaskSetup {
  private final Project project;

  public GithubReleasesTaskSetup(final Project project) {
    this.project = project;
  }

  public void setup() {
    final Task createGithubReleaseTask = project.getTasks().create(
      "createGithubRelease",
      CreateGithubReleaseTask.class
    );
    createGithubReleaseTask.setDescription(
      "Creates a new Github release using the Github API"
    );
  }
}
