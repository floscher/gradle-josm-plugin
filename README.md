# Gradle plugin for developing JOSM plugins

## Getting started
### Prerequisites
Naturally this plugin only makes sense, if you build your JOSM plugin with Gradle.

### Setup

Add the following to the beginning of your `build.gradle` file:

```gradle
plugins {
  id "org.openstreetmap.josm.gradle.plugin" version "0.1.7"
}
```

### Configuration

Add the following to your `build.gradle`:
```gradle
version = '‹pluginVersion›'
josm {
  josmCompileVersion = ‹josmVersionToCompileAgainst›
  manifest {
    description = '‹descriptionOfPlugin›'
    mainClass = '‹fullNameOfMainPluginClass›'
    minJosmVersion = ‹minCompatibleJosmVersion›
  }
}
```
Replace all parts enclosed in `‹›` by the appropriate values.

There are more configuration options you can set in that josm{} block, see [the documentation](https://floscher.github.io/gradle-josm-plugin/groovydoc/current/org/openstreetmap/josm/gradle/plugin/JosmPluginExtension.html#prop_detail) for all available options.

E.g., if your JOSM plugin requires other JOSM plugins, simply add them to your dependencies:
```gradle
josm.manifest.pluginDependencies << ['AwesomePluginName1', 'AwesomePluginName2', 'AwesomePluginName3']
```

### Usage

The main point of using this plugin is, that it allows you to easily fire up a JOSM instance with the current state of your JOSM plugin.

Simply run `./gradlew runJosm` and Gradle does the following for you:
* compiles your JOSM plugin into a \*.jar file
* creates a separate JOSM home directory (`$projectDir/build/.josm/`) in order not to interfere with other preexisting JOSM installations on your system
* puts the JOSM plugins you require into the plugins directory of the separate JOSM home directory
* puts your JOSM plugin also into the plugins directory
* starts the specific JOSM version, which you told Gradle you want to implement and compile against

By default the separate JOSM home directory is kept between separate executions of the `runJosm` task. If you want to regenerate it, execute the `cleanJosm` task.

For external debugging (e.g. using Eclipse), you can use the task `debugJosm`.

## Projects using this Gradle plugin
* [iandees/josm-fieldpapers](https://github.com/iandees/josm-fieldpapers)
* [iandees/josm-mbtiles](https://github.com/iandees/josm-mbtiles)
* [JOSM/geojson](https://github.com/JOSM/geojson)
* [JOSM/Mapillary](https://github.com/JOSM/Mapillary)
