# Gradle plugin for developing JOSM plugins

[![build status](https://img.shields.io/travis/floscher/gradle-josm-plugin/master.svg?style=flat-square)](https://travis-ci.org/floscher/gradle-josm-plugin)
[![latest release](https://img.shields.io/github/tag/floscher/gradle-josm-plugin.svg?style=flat-square)](https://plugins.gradle.org/plugin/org.openstreetmap.josm)
[![license](https://img.shields.io/github/license/floscher/gradle-josm-plugin.svg?style=flat-square)](https://github.com/floscher/gradle-josm-plugin/blob/master/LICENSE)

This Gradle plugin is designed to make development of JOSM plugins easier.

Things like test driving your JOSM plugins before releasing them becomes as easy as typing `./gradlew runJosm` into the command line and the `gradle-josm-plugin` automatically takes care of compiling the current state of your JOSM plugin, getting starting a clean (!) instance of JOSM with only your plugin loaded (and all required plugins, even transitive ones). You'll no longer need to keep a local copy of the JOSM source code in a compatible version and the other JOSM plugins!

See the docs below for many more things you can do. And you don't even need to abandon the Ant build, which is used for most of the JOSM plugins. Option 2 in [the guide for setting up the `gradle-josm-plugin`](docs/Setup.md) explains how a JOSM plugin can support building with either build system (many configuration options can be shared between both).

If you miss something here, found something that's broken, or need more info about this project: Feel free to open issues on this project!

## Getting started

The plugin is available [in the default Gradle plugin repository](https://plugins.gradle.org/plugin/org.openstreetmap.josm).

See [`docs/Setup.md`](docs/Setup.md) for detailed setup instructions.

See [`docs/Tasks.md`](docs/Tasks.md) for information, from which tasks you can choose.

And if you need external dependencies for your plugin, maybe [`docs/Dependencies.md`](docs/Dependencies.md) can help.

If you want to know how i18n works, have a look at [`docs/I18n.md`](docs/I18n.md).

Also, the [KDoc (similar to Javadoc, but for Kotlin)](https://floscher.github.io/gradle-josm-plugin/kdoc/current/gradle-josm-plugin/org.openstreetmap.josm.gradle.plugin.config/) for this Gradle plugin are available online.

## Projects using this Gradle plugin
* [Gubaer/josm-**contourmerge**-plugin](https://github.com/Gubaer/josm-contourmerge-plugin)
* [fieldpapers/josm-**fieldpapers**](https://github.com/fieldpapers/josm-fieldpapers)
* [JOSM/**geojson**](https://github.com/JOSM/geojson)
* [osmlab/**josm-atlas**](https://github.com/osmlab/josm-atlas)
* [qeef/**mapathoner**](https://github.com/qeef/mapathoner)
* [JOSM/**Mapillary**](https://github.com/JOSM/Mapillary)
* [matsim-org/josm-**matsim**-plugin](https://github.com/matsim-org/josm-matsim-plugin)
* [iandees/josm-**mbtiles**](https://github.com/iandees/josm-mbtiles)
* [Gubaer/josm-**scripting**-plugin](https://github.com/Gubaer/josm-scripting-plugin)
* [JOSM/**wikipedia**](https://github.com/JOSM/wikipedia)

## Deprecated links
Alternatively to the default Gradle plugin repository, you can download this plugin from a Maven repository at `https://floscher.github.io/gradle-josm-plugin/maven`.
That link gives a 404 HTTP status, but the repository is still there. The contents can be viewed at https://github.com/floscher/gradle-josm-plugin/tree/gh-pages/maven .

Under https://plugins.gradle.org/plugin/org.openstreetmap.josm.gradle.plugin you can find the versions < `v0.2.0` of the plugin. Later versions have the shorter plugin ID [`org.openstreetmap.josm`](https://plugins.gradle.org/plugin/org.openstreetmap.josm).
