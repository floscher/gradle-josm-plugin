package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.tasks.SourceSet
import org.gradle.language.jvm.tasks.ProcessResources
import org.openstreetmap.josm.gradle.plugin.i18n.DefaultI18nSourceSet
import org.openstreetmap.josm.gradle.plugin.task.LangCompile
import org.openstreetmap.josm.gradle.plugin.task.MoCompile
import org.openstreetmap.josm.gradle.plugin.task.PoCompile
import org.openstreetmap.josm.gradle.plugin.task.ShortenPoFiles

fun SourceSet.setup(project: Project, sdsf: SourceDirectorySetFactory) {
  if (name.isNotEmpty()) {
    // Inspired by https://github.com/gradle/gradle/blob/9d86f98b01acb6496d05e05deddbc88c1e35d038/subprojects/plugins/src/main/java/org/gradle/api/plugins/GroovyBasePlugin.java#L88-L113
    val i18nSourceSet = DefaultI18nSourceSet(this, sdsf)
    DslObject(this).convention.plugins["i18n"] = i18nSourceSet

    // Create "shortenPoFiles" task for the current i18n source set
    project.tasks.create(
      if (name=="main") "shortenPoFiles" else "shorten${name.capitalize()}PoFiles",
      ShortenPoFiles::class.java,
      i18nSourceSet
    )

    val poCompileTask = project.tasks.create(
      getCompileTaskName("po"),
      PoCompile::class.java,
      i18nSourceSet
    )
    val moCompileTask = project.tasks.create(
      getCompileTaskName("mo"),
      MoCompile::class.java,
      poCompileTask,
      i18nSourceSet
    )
    val langCompileTask = project.tasks.create(
      getCompileTaskName("lang"),
      LangCompile::class.java,
      moCompileTask,
      i18nSourceSet
    )
    if ("main" == name) {
      project.extensions.josm.manifest.langCompileTask = langCompileTask
    }

    project.tasks.withType(ProcessResources::class.java).getByName(processResourcesTaskName).also {
      it.from(langCompileTask)
    }
  }
}
