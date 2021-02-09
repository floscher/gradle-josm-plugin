package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.jvm.tasks.ProcessResources
import org.openstreetmap.josm.gradle.plugin.i18n.DefaultI18nSourceSet
import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet
import org.openstreetmap.josm.gradle.plugin.task.LangCompile
import org.openstreetmap.josm.gradle.plugin.task.MoCompile
import org.openstreetmap.josm.gradle.plugin.task.PoCompile
import org.openstreetmap.josm.gradle.plugin.task.ShortenPoFiles
import org.openstreetmap.josm.gradle.plugin.util.josm

/**
 * Add the [I18nSourceSet] and create the associated tasks ([PoCompile], [MoCompile], [LangCompile], [ShortenPoFiles]).
 * This allows this source set to also contain files of types `*.po`, `*.mo` and `*.lang`.
 * @param [project] the [Project] to which this source set belongs
 */
fun SourceSet.setup(project: Project) {
  if (name.isNotEmpty()) {
    // Inspired by https://github.com/gradle/gradle/blob/a6311b0530ca8e0070d85c42c7a44dfa0e872b13/subprojects/plugins/src/main/java/org/gradle/api/plugins/GroovyBasePlugin.java#L96-L143
    val i18nSourceSet: I18nSourceSet = DefaultI18nSourceSet(this, project.objects)
    DslObject(this).convention.plugins["i18n"] = i18nSourceSet

    // Create "shortenPoFiles" task for the current i18n source set
    project.tasks.create(
      if (name==SourceSet.MAIN_SOURCE_SET_NAME) "shortenPoFiles" else "shorten${name.capitalize()}PoFiles",
      ShortenPoFiles::class.java,
      i18nSourceSet
    )

    val poTaskProvider: () -> TaskProvider<PoCompile> = {
      project.tasks.register(getCompileTaskName("po"), PoCompile::class.java, i18nSourceSet)
    }

    val i18nCompileTask = when {
      !i18nSourceSet.po.isEmpty -> poTaskProvider()
      !i18nSourceSet.mo.isEmpty -> project.tasks.register(getCompileTaskName("mo"), MoCompile::class.java, i18nSourceSet)
      !i18nSourceSet.lang.isEmpty -> project.tasks.register(getCompileTaskName("lang"), LangCompile::class.java, i18nSourceSet)
      else -> poTaskProvider()
    }

    project.tasks.withType(ProcessResources::class.java).getByName(processResourcesTaskName) {
      it.from(i18nCompileTask)
    }
  }
}
