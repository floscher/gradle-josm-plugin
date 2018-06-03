package org.openstreetmap.josm.gradle.plugin.setup;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask;
import org.openstreetmap.josm.gradle.plugin.task.PublishToGithubReleaseTask;

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

    final Task publishToGithubReleaseTask = project.getTasks().create(
      "publishToGithubRelease",
      PublishToGithubReleaseTask.class
    );
    publishToGithubReleaseTask.setDescription(
      "Publish a JOSM plugin jar as Github release asset to a " +
      "Github release"
    );
  }
}
