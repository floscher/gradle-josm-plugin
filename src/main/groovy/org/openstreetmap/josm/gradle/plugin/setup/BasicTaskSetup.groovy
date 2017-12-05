package org.openstreetmap.josm.gradle.plugin.setup

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskExecutionException

import org.openstreetmap.josm.gradle.plugin.RunJosmTask

class BasicTaskSetup extends AbstractSetup {

  public void setup() {
    // Clean JOSM
    pro.task(
      [type: Delete, group: 'JOSM', description: 'Delete JOSM configuration in `build/.josm/`'],
      'cleanJosm',
      {t ->
        pro.gradle.projectsEvaluated {
          delete pro.josm.tmpJosmHome
        }
        doFirst {
          pro.logger.lifecycle 'Delete {}…', delete
        }
      }
    )
    // Init JOSM preferences
    pro.task(
      [type: Copy, description: 'Puts a default preferences.xml file into the temporary JOSM home directory'],
      'initJosmPrefs',
      {t ->
        pro.gradle.projectsEvaluated {
          from "${pro.josm.josmConfigDir}"
          into "${pro.josm.tmpJosmHome}"
          include 'preferences.xml'
          if (source.size() <= 0) {
            pro.logger.debug "No default JOSM preference file found in ${pro.josm.josmConfigDir}/preferences.xml."
          }
        }
        doFirst {
          if (new File("${destinationDir}/preferences.xml").exists()) {
            pro.logger.lifecycle "JOSM preferences not copied, file is already present.\nIf you want to replace it, run the task 'cleanJosm' additionally."
            return 0
          }
          pro.logger.lifecycle 'Copy {} to {}…', source.files, destinationDir
        }
      }
    )
    // "Virtual task" that depends on all tasks, which put the desired plugin *.jar files into the plugins directory
    // All RunJosmTasks by default depend on this task.
    pro.task(
      [
        type: Sync,
        description: 'Put all needed plugin *.jar files into the plugins directory. This task copies files into the temporary JOSM home directory.'
      ],
      'updateJosmPlugins',
      {t ->
        pro.gradle.projectsEvaluated {
          t.into "${pro.josm.tmpJosmHome}/plugins"
          // the rest of the configuration (e.g. from where the files come, that should be copied) is done later (e.g. in the file `PluginTaskSetup.groovy`)
        }
      }
    )
    // Standard run-task
    pro.task(
      [
        type: RunJosmTask.class,
        description: 'Runs an independent JOSM instance (version specified in project dependencies) with `build/.josm/` as home directory and the freshly compiled plugin active.'
      ],
      'runJosm'
    )
    // Debug task
    pro.task(
      [type: RunJosmTask.class],
      'debugJosm',
      {t ->
        pro.gradle.projectsEvaluated {
          description 'Runs a JOSM instance like the task `runJosm`, but with JDWP (Java debug wire protocol) active' + (
            pro.josm.debugPort == null
            ? ".\n  NOTE: Currently the `debugJosm` task will error out! Set the property `project.josm.debugPort` to enable it!"
            : ' on port ' + pro.josm.debugPort
          )
          extraInformation '\nThe application is listening for a remote debugging connection on port ' + pro.josm.debugPort + '. It will start execution as soon as the debugger is connected.\n'
          jvmArgs "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + pro.josm.debugPort
        }
        doFirst {
          if (pro.josm.debugPort == null) {
            throw new TaskExecutionException(t, new NullPointerException(
              "You have to set the property `project.josm.debugPort` to the port on which you'll listen for debug output. If you don't want to debug, simply use the task `runJosm` instead of `debugJosm`."
            ));
          }
        }
      }
    )
  }
}
