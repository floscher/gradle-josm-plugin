package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.tasks.SourceSet
import org.gradle.language.jvm.tasks.ProcessResources
import org.openstreetmap.josm.gradle.plugin.i18n.DefaultI18nSourceSet
import org.openstreetmap.josm.gradle.plugin.i18n.io.LangReader
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgId
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgStr
import org.openstreetmap.josm.gradle.plugin.task.LangCompile
import org.openstreetmap.josm.gradle.plugin.task.MoCompile
import org.openstreetmap.josm.gradle.plugin.task.PoCompile
import org.openstreetmap.josm.gradle.plugin.task.ShortenPoFiles
import java.io.File

fun SourceSet.setup(project: Project, sdsf: SourceDirectorySetFactory) {
  if (name != "minJosmVersion" && name.isNotEmpty()) {
    // Inspired by https://github.com/gradle/gradle/blob/9d86f98b01acb6496d05e05deddbc88c1e35d038/subprojects/plugins/src/main/java/org/gradle/api/plugins/GroovyBasePlugin.java#L88-L113
    val i18nSourceSet = DefaultI18nSourceSet(this, sdsf)
    DslObject(this).convention.plugins["i18n"] = i18nSourceSet

    // Create "shortenPoFiles" task for the current i18n source set
    project.tasks.create(
      if (name=="main") "shortenPoFiles" else "shorten${name.capitalize()}PoFiles",
      ShortenPoFiles::class.java,
      { it.sourceSet = i18nSourceSet }
    )

    val poCompileTask = project.tasks.create(getCompileTaskName("po"), PoCompile::class.java, {
      it.sourceSet = i18nSourceSet
    })
    val moCompileTask = project.tasks.create(getCompileTaskName("mo"), MoCompile::class.java, {
      it.sourceSet = i18nSourceSet
      it.poCompile = poCompileTask
    })
    val langCompileTask = project.tasks.create(getCompileTaskName("lang"), LangCompile::class.java, {
      it.sourceSet = i18nSourceSet
      it.moCompile = moCompileTask
    })

    project.tasks.withType(ProcessResources::class.java).getByName(processResourcesTaskName).also {
      it.from(langCompileTask)
      if (this.name == "main") {
        it.doLast {
          if (langCompileTask.didWork) {
            val translations = LangReader().readLangFiles(File(langCompileTask.destinationDir, langCompileTask.subdirectory), project.extensions.josm.i18n.mainLanguage)
            val baseDescription = project.extensions.josm.manifest.description
            if (baseDescription != null) {
              translations.forEach {
                val translatedDescription = it.value[MsgId(MsgStr(baseDescription))]
                if (translatedDescription != null && translatedDescription.strings.isNotEmpty()) {
                  project.extensions.josm.manifest.translatedDescription(it.key, translatedDescription.strings.first())
                }
              }
            }
          }
        }
      }
    }
  }
}
