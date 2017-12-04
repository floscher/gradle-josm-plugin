package org.openstreetmap.josm.gradle.plugin.setup

import org.gradle.api.tasks.Exec
import groovy.io.FileType

class I18nTaskSetup extends AbstractSetup {
  public void setup() {
    pro.task(
      'genSrcFileList',
      {t ->
        def outFile = new File("${pro.buildDir}/srcFileList.txt")
        t.outputs.files(outFile)
        pro.gradle.projectsEvaluated {
          t.inputs.files(pro.sourceSets.main.java.srcDirs)
        }
        doFirst {
          outFile.delete()
          pro.sourceSets.main.java.srcDirs.each { dir ->
            dir.eachFileRecurse(FileType.FILES) { f ->
              outFile << f.absolutePath + "\n"
            }
          }
        }
      }
    )

    pro.task(
      [type: Exec, group: 'JOSM-i18n', description: 'Extracts translatable strings from the source code', dependsOn: 'genSrcFileList'],
      'i18n-xgettext',
      {t ->
        def outDir = new File("${pro.buildDir}/po")
        def outBaseName = "plugin_${pro.name}"
        t.inputs.files(pro.sourceSets.main.java.srcDirs)
        t.outputs.dir(outDir)

        workingDir "${pro.projectDir}"
        executable 'xgettext'
        args '--from-code=UTF-8', '--language=Java',
        "--files-from=${pro.buildDir}/srcFileList.txt",
        "--output-dir=${outDir.absolutePath}",
        "--default-domain=plugin_${pro.name}",
        "--package-name=plugin/${pro.name}",
        '-k', '-ktrc:1c,2', '-kmarktrc:1c,2', '-ktr', '-kmarktr', '-ktrn:1,2', '-ktrnc:1c,2,3'

        pro.gradle.projectsEvaluated {
          args "--package-version=${pro.version}"
        }

        doFirst {
          outDir.mkdirs()
          print executable
          args.each { a ->
            print ' ' + a
          }
          println ''
        }
        doLast {
          new File(outDir, outBaseName + ".po").renameTo(new File(outDir, outBaseName + ".pot"))
        }
      }
    )
  }
}
