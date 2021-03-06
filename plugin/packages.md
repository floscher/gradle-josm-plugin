# Module plugin

This module contains the core of the `gradle-josm-plugin`. It is packaged together with the libraries in the subprojects `:dogfood` and `:i18n`.

# Package org.openstreetmap.josm.gradle.plugin

The main plugin class [JosmPlugin] and some extensions for [org.gradle.api.Project].

In Java the extensions can be accessed via `ProjectKt`, in Kotlin via the [org.gradle.api.Project] object.

# Package org.openstreetmap.josm.gradle.plugin.config

Classes for configuring the Gradle build of your JOSM plugin.

[JosmPluginExtension] is the main class, which can be accessed in your Gradle build via `project.josm`.

[I18nConfig] and [JosmManifest] provide some specialized options for internationalization and the `MANIFEST.MF` file of your JOSM plugin. These are accessible via `project.josm.i18n` and `project.josm.manifest`.

# Package org.openstreetmap.josm.gradle.plugin.github

Classes for interacting with the Github API (for creating/publishing releases).

# Package org.openstreetmap.josm.gradle.plugin.i18n

The source set definition for the different translation formats (*.lang, *.po and *.mo).

# Package org.openstreetmap.josm.gradle.plugin.io

Classes for reading JOSM plugin lists.

# Package org.openstreetmap.josm.gradle.plugin.task

Reusable Gradle tasks.

# Package org.openstreetmap.josm.gradle.plugin.task.github

Tasks used for publishing to GitHub releases.

# Package org.openstreetmap.josm.gradle.plugin.util

Some utilities to reuse across the project.
