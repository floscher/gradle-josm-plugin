package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.tasks.JavaExec
import org.openstreetmap.josm.gradle.plugin.JosmPlugin

/**
 * A task that can execute a JOSM instance. Both the {@code runJosm} task and the {@code debugJosm} task extend this type of task.
 */
class RunJosmTask extends JavaExec {
  /**
   * Text that should be displayed in the console output right before JOSM is started up. Defaults to the empty string.
   */
  private String extraInformation = ''
  /**
   * Instantiates a new task for running a JOSM instance.
   * By default the source set <code>main</code> is added to
   */
  public RunJosmTask() {
    def arguments = project.hasProperty('josmArgs') ? project.josmArgs.split('\\\\') : []
    arguments << "--load-preferences=" + new File(JosmPlugin.currentProject.buildDir, "/josm-custom-config/requiredPlugins.xml").toURI().toURL().toString()

    group 'JOSM'
    main 'org.openstreetmap.josm.gui.MainApplication'
    args arguments
    shouldRunAfter project.tasks.cleanJosm

    dependsOn project.tasks.updateJosmPlugins
    project.gradle.projectsEvaluated {
      systemProperties['josm.home'] = project.josm.tmpJosmHome
      classpath = project.sourceSets.main.runtimeClasspath

      doFirst {
        println "Running version ${project.version} of ${project.name}"
        println "\nUsing JOSM version " + project.josm.josmCompileVersion

        println '\nThese system properties are set:'
        for (def entry : systemProperties.entrySet()) {
          println entry.key + " = " + entry.value
        }

        if (args.size() <= 0) {
          println '\nNo command line arguments are passed to JOSM.\nIf you want to pass arguments to JOSM add \'-PjosmArgs="arg0\\\\arg1\\\\arg2\\\\..."\' when starting Gradle from the commandline (separate the arguments with double-backslashes).'
        } else {
          println '\nPassing these arguments to JOSM:'
          println args.join('\n')
        }

        print this.extraInformation

        println '\nOutput of JOSM starts with the line after the three equality signs\n==='
      }
    }
  }

  /**
   * Set the text that should be displayed right before the console output of JOSM when starting JOSM.
   * This is used e.g. to display the remote debugging port.
   */
  public void setExtraInformation(final String extraInformation) {
    this.extraInformation = extraInformation;
  }

  /**
   * Getter method for the field {@code extraInformation}
   */
  public String getExtraInformation() {
    return this.extraInformation
  }

}
