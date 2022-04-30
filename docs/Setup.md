# Setup

There are three ways you can set up a JOSM plugin project:

* **If it should be built exclusively with Ant:**

  Why are you here? Just follow the [Ant setup guide][1].

* **If the project should be only built with Gradle:**

  In this case, follow steps 1 and 2a

* **If it should be possible to build with either Ant or Gradle:**

  If not already done, set the Ant build up as described in the [Ant setup guide][1]. Then follow steps 1 and 2b.

## Step 1: Setup for all projects

Add the following to the beginning of your `build.gradle.kts`:
```kotlin
plugins {
  id("org.openstreetmap.josm").version("‹insertCurrentVersionNumber›")
}
```
Replace `‹insertCurrentVersionNumber›` with the [latest available version][2].

## Step 2a: Gradle-exclusive setup

Add the following to your `build.gradle.kts`:
```kotlin
josm {
  pluginName = "MyAwesomePluginName"
  // debugPort = 1729 // choose a random port for your project (to avoid clashes with other projects)
  josmCompileVersion = 1234
  manifest {
    description = "The description of my awesome plugin"
    mainClass = "org.openstreetmap.josm.plugins.myawesomeplugin.MyAwesomePlugin"
    minJosmVersion = 1234
    // author = "John Doe"
    // canLoadAtRuntime = true
    // iconPath = "path/to/my/icon.svg"
    // loadEarly = false
    // loadPriority = 50
    // pluginDependencies += setOf("apache-commons", "apache-http")
    // website = java.net.URL("https://example.org")
    // oldVersionDownloadLink(123, "v1.2.0", java.net.URL("https://example.org/download/v1.2.0/MyAwesomePlugin.jar"))
    // oldVersionDownloadLink( 42, "v1.0.0", java.net.URL("https://example.org/download/v1.0.0/MyAwesomePlugin.jar"))

    // to populate the 'Class-Path' attribute in the JOSM plugin manifest invoke
    // the function 'classpath', i.e.
    //   classpath "foo.jar"
    //   classpath "sub/dir/bar.jar"
    // This results in 'Class-Path: foo.jar sub/dir/bar.jar' in the
    // manifest file. Added class path entries must not contain blanks.
  }
  // i18n {
  //   bugReportEmail = "me@example.com"
  //   copyrightHolder = "John Doe"
  // }
}
```
Some of these options are commented out, that means that they are optional, the others have to be set.

For documentation on what each of these means, see the documentation for [`josm{ }`][3], [`manifest{ }`][4] and for [`i18n{ }`][5].


## Step 2b: Dual-setup of Ant and Gradle

Remove each of the properties below from your `build.xml` file and put them into a `gradle.properties` file, as done below (except `plugin.description`: You need to **copy** that over, so it is in both files, or it won't get translated). These are by the way all example values, replace with your own ones:
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
# plugin.link=https://example.org/path/to/doc/website
```
In the `build.xml` file put the following where the properties were defined previously:
```xml
<!-- edit the properties of this plugin in the file `gradle.properties` -->
<property file="${basedir}/gradle.properties"/>
```

These properties define the contents of the manifest that will be distributed in your plugin \*.jar file.

Some of these properties are commented out, these are optional and don't need to be set.

For the meaning of each property, refer to [the section of the plugin development guide about the MANIFEST][6]. Only `plugin.compile.version` is not mentioned there, it simply defines the version of JOSM, against which you want to compile and it is also used to determine, which JOSM version you want to run with the tasks `runJosm` and `debugJosm`.

The property `plugin.main.version` is also used to test compilation against that version of JOSM using the task `minJosmVersionClasses` (see [Tasks documentation](./Tasks.md)).

The property `plugin.requires` is used to automatically put required JOSM plugins on the classpath.

That's it, your project is now set up to build with the `gradle-josm-plugin`.

**Note:** You have the choice to also set up like in step 2a, but that will override the settings from your `gradle.properties` for the Gradle build. Ant on the other hand will still use the settings from the `gradle.properties`.

[1]: https://josm.openstreetmap.de/wiki/DevelopersGuide/DevelopingPlugins
[2]: https://plugins.gradle.org/plugin/org.openstreetmap.josm
[3]: https://josm.gitlab.io/gradle-josm-plugin/kdoc/latest/plugin/plugin/org.openstreetmap.josm.gradle.plugin.config/-josm-plugin-extension/
[4]: https://josm.gitlab.io/gradle-josm-plugin/kdoc/latest/plugin/plugin/org.openstreetmap.josm.gradle.plugin.config/-josm-manifest/
[5]: https://josm.gitlab.io/gradle-josm-plugin/kdoc/latest/plugin/plugin/org.openstreetmap.josm.gradle.plugin.config/-i18n-config/
[6]: https://josm.openstreetmap.de/wiki/DevelopersGuide/DevelopingPlugins#ThemanifestfileforaJOSMplugin
