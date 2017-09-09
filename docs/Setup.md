# Setup

If you don't want to also build with Ant, you should set up your JOSM plugin in your `build.gradle` file (option 1).

If your project can also be built with the Ant build system, you should follow option 2, which puts the set up for Ant and Gradle into the `gradle.properties` file, so that both pick up the same options.

But let's start with what you have to set up in both of these cases:

## Setup for all projects

Add the following to the beginning of your `build.gradle`:
```gradle
plugins {
  id 'org.openstreetmap.josm.gradle.plugin' version '‹insertCurrentVersionNumber›'
}
```
(replace `‹insertCurrentVersionNumber›` by the latest version number that is displayed at [the Gradle plugin repository](https://plugins.gradle.org/plugin/org.openstreetmap.josm.gradle.plugin))

## Option 1: My project does not have a `build.xml` file in the project root

Add the following to your `build.gradle`:
```gradle
version = 'v1.2.3'
project.archivesBaseName = 'MyAwesomePluginName'
josm {
  // debugport = ‹insertDebugPortNumber›
  josmCompileVersion = 1234
  manifest {
    description = 'The description of my awesome plugin'
    mainClass = 'org.openstreetmap.josm.plugins.myawesomeplugin.MyAwesomePlugin'
    minJosmVersion = 1234
    // author = 'John Doe'
    // canLoadAtRuntime = true
    // iconPath = 'path/to/my/icon.svg'
    // loadEarly = false
    // loadPriority = 50
    // pluginDependencies << 'apache-commons' << 'apache-http'
    // website = new URL('https://example.org')
    // oldVersionDownloadLink 123, 'v1.2.0', new URL('https://example.org/download/v1.2.0/MyAwesomePlugin.jar')
    // oldVersionDownloadLink  42, 'v1.0.0', new URL('https://example.org/download/v1.0.0/MyAwesomePlugin.jar')
  }
}
```
Some of these options are commented out, that means that they are optional, the others have to be set.

For documentation on what each of these means, see the documentation for [`josm{ }`](https://floscher.github.io/gradle-josm-plugin/groovydoc/current/org/openstreetmap/josm/gradle/plugin/JosmPluginExtension.html) and for [`manifest{ }`](https://floscher.github.io/gradle-josm-plugin/groovydoc/current/org/openstreetmap/josm/gradle/plugin/Manifest.html).



## Option 2: My project has a `build.xml` file in the project root

Remove each of the properties below from your `build.xml` file and put them into a `gradle.properties` file (except `plugin.description`, you need to **copy** that over, or it won't get translated), like this. These are by the way all example values, replace with your own ones:
```properties
plugin.class=org.openstreetmap.josm.plugins.myawesomeplugin.MyAwesomePlugin
plugin.compile.version=1234
plugin.description=The description of my awesome plugin
plugin.main.version=1234
# plugin.author=John Doe
# plugin.canloadatruntime=true
# plugin.early=false
# plugin.icon=path/to/my/icon.svg
# plugin.requires=apache-commons;apache-http
# plugin.stage=50
```
In the `build.xml` file put the following where the properties were defined previously:
```xml
<!-- edit the properties of this plugin in the file `gradle.properties` -->
<property file="${basedir}/gradle.properties"/>
```

These properties define the contents of the manifest that will be distributed in your plugin \*.jar file.

Some of these properties are commented out, these are optional and don't need to be set.

For the meaning of each property, refer to [the section of the plugin development guide about the MANIFEST](https://josm.openstreetmap.de/wiki/DevelopersGuide/DevelopingPlugins#ThemanifestfileforaJOSMplugin). Only `plugin.compile.version` is not mentioned there, it simply defines the version of JOSM, against which you want to compile and it is also used to determine, which JOSM version you want to run with the tasks `runJosm` and `debugJosm`.

The property `plugin.main.version` is also used to test compilation against that version of JOSM using the task `minJosmVersionClasses` (see [Tasks documentation](./Tasks.md)).

The property `plugin.requires` is used to automatically put required JOSM plugins on the classpath.

That's it, your project is now set up to build with the `gradle-josm-plugin`.

**Note:** You have the choice to also set up like in Option 1, but that will override the settings from your `gradle.properties` for the Gradle build. Ant on the other hand will still use the settings from the `gradle.properties`.
