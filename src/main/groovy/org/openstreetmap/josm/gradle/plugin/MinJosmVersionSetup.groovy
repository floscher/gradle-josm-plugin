package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.GradleException
import org.gradle.api.Project

public final class MinJosmVersionSetup {
  private final Project pro

  public MinJosmVersionSetup(Project pro) {
    this.pro = pro
  }

  public void setup() {
    pro.configurations {
      minJosmVersionImplementation.extendsFrom(requiredPlugin)
    }

    pro.task(
      'addMinJosmVersionDependency',
      {t ->
        doFirst {
          // Find the next available version from the one specified in the manifest
          def minJosmVersion = getNextJosmVersion(pro.josm.manifest.minJosmVersion)
          if (minJosmVersion == null) {
            throw new GradleException("Could not determine the minimum required JOSM version from the given version number '" + pro.josm.manifest.minJosmVersion + "'")
          }
          pro.logger.lifecycle 'Use JOSM version {} for compiling against the minimum required version', minJosmVersion
          pro.dependencies.add('minJosmVersionImplementation', 'org.openstreetmap.josm:josm:'+ minJosmVersion)
        }
      }
    )

    pro.gradle.projectsEvaluated {
      pro.sourceSets{
        minJosmVersion {
          java {
            srcDirs = pro.sourceSets.main.java.srcDirs
          }
          resources {
            srcDirs = pro.sourceSets.main.resources.srcDirs
            includes = pro.sourceSets.main.resources.includes
          }
        }
      }
      pro.minJosmVersionClasses.setGroup('JOSM')
      pro.minJosmVersionClasses.setDescription('Try to compile against the version of JOSM that is specified in the manifest as the minimum compatible version')
      pro.compileMinJosmVersionJava.dependsOn pro.addMinJosmVersionDependency
    }


  }

  /**
   * Returns the next JOSM version available for download for a version number given as string
   */
  private Integer getNextJosmVersion(String startVersionString) {
    def startVersion = Integer.parseInt(startVersionString)
    for (def i = startVersion; i < startVersion + 50; i++) {
      pro.logger.lifecycle "Checking if JOSM version {} is available for download…", i
      URL u1 = new URL('https://josm.openstreetmap.de/download/josm-snapshot-' + i + '.jar')
      URL u2 = new URL('https://josm.openstreetmap.de/download/Archiv/josm-snapshot-' + i + '.jar')
      HttpURLConnection con1 = (HttpURLConnection) u1.openConnection()
      con1.setRequestMethod('HEAD')
      HttpURLConnection con2 = (HttpURLConnection) u2.openConnection()
      con2.setRequestMethod('HEAD')
      if (con1.getResponseCode() == 200 || con2.getResponseCode() == 200) {
        return i
      }
    }
    return null
  }
}
