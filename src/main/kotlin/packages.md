# Package org.openstreetmap.josm.gradle.plugin.config

Classes for configuring the Gradle build of your JOSM plugin.

[JosmPluginExtension] is the main class, which can be accessed in your Gradle build via `project.josm`.

[I18nConfig] and [JosmManifest] provide some specialized options for internationalization and the `MANIFEST.MF` file of your JOSM plugin. These are accessible via `project.josm.i18n` and `project.josm.manifest`.
