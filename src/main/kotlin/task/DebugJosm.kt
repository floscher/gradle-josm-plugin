package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskExecutionException
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension
import org.openstreetmap.josm.gradle.plugin.josm

/**
 * The same as [RunJosmTask], but the JOSM instance is debuggable via JDWP (Java debug wire protocol) on the port
 * configured at [JosmPluginExtension.debugPort].
 */
open class DebugJosm : RunJosmTask() {
  @Input
  private var debugPort: Int? = null
  init {
    project.afterEvaluate {
      debugPort = project.extensions.josm.debugPort
      description = "Runs a JOSM instance like the task `runJosm`, but with JDWP (Java debug wire protocol) active" +
        if (debugPort == null) {
          ".\n  NOTE: Currently the `debugJosm` task will error out! Set the property `project.josm.debugPort` to enable it!"
        } else {
          " on port $debugPort"
        }

      extraInformation = "\nThe application is listening for a remote debugging connection on port $debugPort. " +
        "It will start execution as soon as the debugger is connected.\n"

      jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=$debugPort")
    }

    doFirst {
      if (debugPort == null) {
        throw TaskExecutionException(this, NullPointerException(
          "You have to set the property `project.josm.debugPort` to the port on which you'll listen for debug output. " +
          "If you don't want to debug, simply use the task `runJosm` instead of `debugJosm`."
        ));
      }
    }
  }
}
