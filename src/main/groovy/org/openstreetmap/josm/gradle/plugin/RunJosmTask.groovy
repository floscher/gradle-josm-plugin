package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.tasks.JavaExec
import org.gradle.api.artifacts.SelfResolvingDependency

class RunJosmTask extends JavaExec {
  def String extraInformation = ''
  public RunJosmTask() {
    group 'JOSM'
    main 'org.openstreetmap.josm.gui.MainApplication'
    args (project.hasProperty('josmArgs') ? project.josmArgs.split('\\\\') : [])
    shouldRunAfter project.tasks.cleanJosm

    dependsOn project.tasks.updateJosmPlugins
    project.gradle.projectsEvaluated {
      systemProperties['josm.home'] = project.josm.tmpJosmHome
      classpath = project.sourceSets.main.runtimeClasspath

      doFirst {
        println "Running version ${project.version} of ${project.name}"
        for (def dep : project.configurations.runtimeClasspath.allDependencies) {
          if ("josm".equals(dep.name)) { // TODO: Is there a better way of finding the JOSM dependency than by name?
            println "\nUsing JOSM version " + dep.version
          }
        }

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

        print extraInformation

        println '\nOutput of JOSM starts with the line after the three equality signs\n==='
      }
    }
  }
}
