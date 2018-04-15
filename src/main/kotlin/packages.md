# Package org.openstreetmap.josm.gradle.plugin

The main plugin class [JosmPlugin] and some extensions for [org.gradle.api.Project].

In Java the extensions can be accessed via `ProjectKt`, in Kotlin via the [org.gradle.api.Project] object.

# Package org.openstreetmap.josm.gradle.plugin.config

Classes for configuring the Gradle build of your JOSM plugin.

[JosmPluginExtension] is the main class, which can be accessed in your Gradle build via `project.josm`.

[I18nConfig] and [JosmManifest] provide some specialized options for internationalization and the `MANIFEST.MF` file of your JOSM plugin. These are accessible via `project.josm.i18n` and `project.josm.manifest`.

# Package org.openstreetmap.josm.gradle.plugin.i18n

The source set definition for the different translation formats (*.lang, *.po and *.mo).

# Package org.openstreetmap.josm.gradle.plugin.i18n.io

A reader for *.mo files and a writer for JOSM's *.lang files

# Package org.openstreetmap.josm.gradle.plugin.task

Reusable Gradle tasks.
