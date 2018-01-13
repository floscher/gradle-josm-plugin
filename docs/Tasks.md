# Tasks

This is a list of some Gradle tasks, which the `gradle-josm-plugin` adds to your build.

If you want to do a full build (including unit tests and other tools you set up, like SpotBugs or PMD), use `./gradlew build` like you would also do without the plugin.

To see a list of available tasks, run `./gradlew tasks` in your project root and take special note of the part titled `JOSM tasks`.

> **Note:** Did you know? You can [abbreviate Gradle tasks](https://docs.gradle.org/4.1/userguide/tutorial_gradle_command_line.html#sec:task_name_abbreviation). E.g. for `runJosm` you could write `runJ` or even `rJ` as long as your abbreviation is not ambiguous.

## runJosm
```bash
./gradlew runJosm
```
This task starts up an isolated JOSM instance independent of any JOSM instances that you may have already installed (you do **not** need to have JOSM installed, Gradle will take care of downloading and running JOSM).

The isolation from other JOSM instances is achieved by using a separate `JOSM_HOME` directory located at `$projectDir/build/.josm/`. By default this directory persists between different executions of the task `runJosm`. If you want to clear this directory to start with a fresh JOSM instance, run the task `cleanJosm` before or together with `runJosm`.

The JOSM version that is used, is the one you specify to compile your plugin against (see `josm.josmCompileVersion` in [the Setup documentation](./Setup.md)).

Your plugin is automatically compiled and activated in that JOSM instance, if you didn't do that before.

Also all plugins that your plugin requires are automatically activated in that JOSM instance.

## cleanJosm
```bash
./gradlew runJosm
```
Deletes the `JOSM_HOME` directory located at `$projectDir/build/.josm/`, which is used for the task `runJosm`.

## debugJosm
```bash
./gradlew debugJosm
```
The same as `runJosm`, but this task has remote debugging over JDWP (Java Debug Wire Protocol) enabled and waits, until someone listens at the port specified in [the Setup documentation](./Setup.md).
This task fails if you have not specified, on which port you want to listen.

## minJosmVersionClasses
```bash
./gradlew minJosmVersionClasses
```
By executing this task you try to compile against the minimum JOSM version that your plugin is compatible with, according to your manifest (see `josm.manifest.minJosmVersion` in [the Setup documentation](./Setup.md)).

## localDist
```bash
./gradlew localDist
```
This task creates a 'plugin site' inside your `build/localDist/` directory. Point any JOSM instance at the URL, that this task reports to you and you can "download" your plugin from your `build/` directory into that JOSM instance.

The task `runJosm` is the preferred (and easier) method to test drive your plugin, but in some circumstances you might not want to test in an isolated environment, but in your normal JOSM setup. If that's the case, this task is for you.

## generatePot
```bash
./gradlew generatePot
```
This task extracts the translatable strings from your source code and writes them to the file `$buildDir/po/josm-plugin_MyAwesomePlugin.pot`.
> **Note:** You have to have `xgettext` installed on your machine in order for this task to work.
