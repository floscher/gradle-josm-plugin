package org.openstreetmap.josm.gradle.plugin.setup

import org.gradle.api.tasks.Exec
import groovy.io.FileType
import java.time.Year

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
          outFile.parentFile.mkdirs()
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
        def outBaseName = "josm-plugin_${pro.name}"
        t.inputs.files(pro.sourceSets.main.java.srcDirs)
        t.outputs.dir(outDir)

        workingDir "${pro.projectDir}"
        executable 'xgettext'
        args '--from-code=UTF-8', '--language=Java',
        "--files-from=${pro.buildDir}/srcFileList.txt",
        "--output-dir=${outDir.absolutePath}",
        "--default-domain=${outBaseName}",
        "--package-name=josm-plugin/${pro.name}",
        '-k', '-ktrc:1c,2', '-kmarktrc:1c,2', '-ktr', '-kmarktr', '-ktrn:1,2', '-ktrnc:1c,2,3'

        pro.gradle.projectsEvaluated {
          args "--package-version=${pro.version}"
          if (pro.josm.i18n.bugReportEmail != null) {
            args '--msgid-bugs-address=' + pro.josm.i18n.bugReportEmail
          }
          if (pro.josm.i18n.copyrightHolder != null) {
            args '--copyright-holder=' + pro.josm.i18n.copyrightHolder
          }
        }

        doFirst {
          outDir.mkdirs()
          println executable
          args.each { a ->
            println "  " + a
          }
        }
        doLast {
          moveFileAndReplaceStrings(
            new File(outDir, outBaseName + ".po"),
            new File(outDir, outBaseName + ".pot"),
            [
              "(C) YEAR": "(C) " + Year.now().value,
              "charset=CHARSET": "charset=UTF-8"
            ])
        }
      }
    )
  }
  private void moveFileAndReplaceStrings(final File src, final File dest, final Map<String,String> replacements) {
    dest.withWriter { writer ->
      src.eachLine { line ->
        final String[] keys = replacements.keySet().toArray(new String[replacements.size()])
        for (final String key : keys) {
          if (line.contains(key) && replacements.containsKey(key)) {
            line = line.replace(key, replacements.get(key))
            replacements.remove(key)
          }
        }
        writer << line + "\n"
      }
    }
    src.delete()
  }
}
