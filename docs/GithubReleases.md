# GitHub Releases for JOSM plugins

> **NOTE:** Publishing to GitHub releases is currently in beta stage. Expect this guide to change unexpectedly or be out of date until this notice is removed.

## Intro

### What are GitHub releases?

[GitHub releases][github-releases] are a way of packaging and providing software to users. GitHub provides a [REST API][github-rest-api] to manage GitHub releases.

### How does JOSM pickup a plugin from a GitHub release?
The JOSM team maintains a public directory with metadata about available plugins. It consists of a simple concatenation of the `MANIFEST`-files included in the plugin `jar`-files. You can look it up [here][josm-plugin-list]. A JOSM instance on a client computer reads downloads and parses this list. If a user decides to [install a plugin][josm-plugin-preferences], JOSM downloads the plugin `jar` from the appropriate download URL included in the plugin metadata in this list.

A plugin `MANIFEST` includes two kinds of download URLs:

1. the default download URL

   This is the download URL of the latest plugin release. Usually, it doesn't include neither a version number nor a release label. 
   For instance, the default download URL for the `scripting` plugin is 
   ```
   https://raw.githubusercontent.com/Gubaer/josm-scripting-plugin/deploy/dist/scripting.jar
   ```
   (you can look it up [here][josm-plugin-list])

2. download URLs for plugins releases compatible with JOSM releases newer than a specific reference JOSM release

   The `scripting` plugin, for instance, is available for the JOSM versions 5315, 8024, 12875, 13007, etc.
   and the `MANIFEST` includes specific download URLs for these JOSM versions
   ```
   5315_Plugin-Url: 30000;https://raw.github.com/Gubaer/josm-scripting-plugin/for-josm-5315/dist/scripting.jar
   8024_Plugin-Url: 30710;https://raw.github.com/Gubaer/josm-scripting-plugin/for-josm-8024/dist/scripting.jar
   12875_Plugin-Url: 30772;https://raw.github.com/Gubaer/josm-scripting-plugin/for-josm-12875/dist/scripting.jar
   13007_Plugin-Url: 30775;https://raw.github.com/Gubaer/josm-scripting-plugin/for-josm-13007/dist/scripting.jar
   ```
   (you can look it up [here][josm-plugin-list]) 

### How to add a new JOSM plugin to the plugin directory? <a id="how-to-add-plugin-to-directory"></a>

Add a stable, release independent download URL for the latest release of the new plugin to [this page][josm-plugin-sources] in the JOSM wiki.

Every 10 minutes, a script in the backend infrastructure of the JOSM development team fetches the `jar`-file from this download URL, extracts the `MANIFEST` file and updates the [directory with the plugin metadata][josm-plugin-list].

## Managing GitHub releases for your plugin

The `gradle-josm-plugin` includes tasks to create GitHub releases and to upload a plugin jar as GitHub release asset.

### Release configuration in `releases.yml` file

You have to maintain a configuration file for the plugin releases. It contains a list of _versioned releases_ with a release label, usually following the the conventions of [semantic versioning](https://semver.org/)

Its default name is `releases.yml` and its default location is the project root.

To configure another location, set the gradle property `josm.github.releasesConfig` directly in `build.gradle`:
 ```groovy
 josm {
   github {
     releasesConfig=file("path/to/my_releases.yml")
   }
 }
 ```

Here is annotated example `releases.yml`:
```yml
# A list of releases
#
# Entries should be ordered chronologically by release date (from oldest release at the top and the most recent release at the bottom of the file)
#
releases:
  # the first entry in the releases list
  - label: v0.0.1                     # MANDATORY: the release label
    minJosmVersion: 1234              # MANDATORY: the lowest numeric josm version
                                      # that this release is compatible with

  # the second entry in the releases list
  - label: v0.0.2                     # MANDATORY:
    minJosmVersion: 5678        # MANDATORY:
    description: a description        # OPTIONAL
    name: a name for the release      # OPTIONAL

  # ... more entries in the releases list 
```


### Create a GitHub release for your plugin ...

#### for the current build

This creates a GitHub release for the plugin version currently configured with the project property `version`.

`releases.yml` has to include a release specification for the `label` given by the project property `version`.

```yml
# releases.yml
releases:
  - label: v0.0.1
    minJosmVersion: 1234

  # more ...
```

Create the release with the following command:
```bash
$ ./gradlew createGithubRelease
```

### for a specific label, using the command line

This creates a GitHub release with the label `v0.0.1`. Note that `releases.yml` must include an entry for this label.

```yml
# releases.yml
releases:
  # ...
  - label: v0.0.1
    # ...

  # more ...
```
```bash
$ ./gradlew createGithubRelease --release-label v0.0.1
```
### for a specific release, configured in `build.gradle`
```yml
# releases.yml
releases:
  # ...
  - label: v0.0.1
    # ...

  # ...
```
```groovy
// build.gradle
createGithubRelease {
    // this configures the plugin release
    releaseLabel = "v0.0.1"
}
```
```bash
$ ./gradlew createGithubRelease
```

You can also define your own task:

```groovy
// build.gradle
import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask
task createMyRelease(type: CreateGithubReleaseTask){
    releaseLabel = "v0.0.1"
}
```

```bash
$ ./gradlew createMyRelease
```

### Publish the plugin jar to a GitHub release

#### Publish to the release with the current release label

Publishes the current plugin jar as release asset to the GitHub release with the label given by the project property `version`. Note that
  * `releases.yml` must include an entry for this label
  * a GitHub release for this label has to exist

```yml
# releases.yml
releases:
  - label: v0.0.1
    minJosmVersion: 1234
    # ...
  # more ...
```
```bash
$ ./gradlew publishToGithubRelease
```

#### Publish to a GitHub release with a specific label, using the command line

Publishes the current plugin jar as release asset to the Github release with the 
label `v0.0.1`.
The local name and path of the plugin jar is derived from the name and location of the jar built by the gradle `jar` task. The name of the published jar is the same as the name  of the local jar.

```bash
$ ./gradlew publishToGithubRelease --release-label v0.0.1
```
#### Publish to a GitHub release with a specific label, configure in `build.gradle`

Publishes the current plugin jar as release asset to the GitHub release with the label `v0.0.1`.
The local name and path of the plugin jar is derived from the name and location of the jar built by the gradle `jar` task. The name of the published jar is the same as the name  of the local jar.

```groovy
// build.gradle
publishToGithubRelease {
    releaseLabel = "v0.0.1"
}
```
```bash
$ ./gradlew publishToGithubRelease --release-label v0.0.1
```
You can also define your own task:
```groovy
// build.gradle
import org.openstreetmap.josm.gradle.plugin.task.PublishToGithubReleaseTask
task publishMyRelease(type: PublishToGithubReleaseTask){
    releaseLabel = "v0.0.1"
}
```
```bash
$ ./gradlew publishMyRelease
```

#### Configure the asset to be published

You can configure the local path of the published `jar` and/or the remote name of the `jar`-asset in the GitHub release.

In `build.gradle`:

```groovy
// build.gradle
publishToGithubRelease {
    localJarPath="/path/to/my/asset/plugin.jar"
    remoteJarName="my_other_name.jar"
}
```


On the command line: 
```bash
$ ./gradlew publishToGithubRelease \
    --local-jar-path /path/to/my/asset/plugin.jar \
    --remote-jar-name my_other_name.jar
```

## Configuration options

A configuration option is derived from command line arguments, properties, and environment variables in the following order (the first of these options that has a non-blank value is used):

1. command line argument
2. task configuration in `build.gradle`
3. properties in the JosmPluginExtension (`josm {}` block in `build.gradle`)
4. gradle property (only some properties)
5. environment variable
6. hard coded default value

### common environment variables
| environment variable | description |
| --------------------- | ----------- |
| `GITHUB_REPOSITORY_OWNER`       | the name of the github user |
| `GITHUB_ACCESS_TOKEN`       | the Github access token |
| `GITHUB_REPOSITORY_NAME`       | the name of the github repository |
| `GITHUB_API_URL` | the base API URL for the Github releases API. Defaults to `https://api.github.com` |
| `GITHUB_UPLOAD_URL` | the base API URL to upload release assets. Defaults to `https://uploads.github.com` |

The access token can also be configured using the Gradle property `josm.github.accessToken`. **Make sure you don't commit it to the git repository, since you should keep the value a secret!**
You could put it into `~/.gradle/gradle.properties` or use the environment variable instead.


## Sample and template configuration files

### template for a script to set environment variables
```bash
#!/bin/bash
#
# save as 'github.env' and load using 'source github.env'
#

# the GitHub account that owns the repository
#export GITHUB_REPOSITORY_OWNER=a-user-name

# the GitHub access token
#export GITHUB_ACCESS_TOKEN=asldiu0w98357oasjf

# the GitHub repository name
#export GITHUB_REPOSITORY_NAME=my-repo

# the GitHub upload URL if different from https://uploads.github.com
#export GITHUB_API_URL=https://api.my-github-host.test

#  the GitHub upload URL if different from https://uploads.github.com
# export GITHUB_UPLOAD_URL=https://uploads.my-github-host.test

# the base GitHub URL. Defaults to http://github.com 
# export GITHUB_MAIN_URL=http://my-github.test
```

### template for `build.gradle`
```groovy
plugins {
    id 'org.openstreetmap.josm'
    id 'java'
    id 'groovy'
    id 'eclipse'
}

version="0.0.5"      //  the current release label

//
josm {
  github { // for more configuration options, see https://floscher.gitlab.io/gradle-josm-plugin/kdoc/latest/gradle-josm-plugin/org.openstreetmap.josm.gradle.plugin.config/-github-config/index.html
    repositoryOwner = "a-github-user"
    repositoryName = "my-repo"
    // the access token is configured either through the environment variable `GITHUB_ACCESS_TOKEN`,
    // or by adding the property `josm.github.accessToken` to the file `~/.gradle/gradle.properties`
  }
  josmCompileVersion = "latest"
  manifest {
    // if true, the plugin will include download URLs for GitHub
    // release assets in the MANIFEST file
    includeLinksToGithubReleases = true
  }
}

// uncomment options to configure the provided task 'createGithubRelease'

createGithubRelease {
    // optional. if different from the project 'version'
    //releaseLabel = "v0.0.5-GA"

    // optional. if different from 'master'
    //targetCommitish="deploy"
}



// uncomment options to configure the provided task 'publishToGithubRelease'

publishToGithubRelease {
    // optional. if different from the project 'version'
    //releaseLabel = "v0.0.5-GA"

    // optional. if different from 'master'
    //targetCommitish="deploy"
    
    // optional. if different from the standard path where gradle build
    // the jar
    //localJarPath="/full/path/to/the/local/my-plugin.jar"
    
    // optional. if different from the name of the jar file built locally
    //remoteJarName="my-plugin.jar"
}


[github-releases]: https://help.github.com/articles/about-releases/
[github-rest-api]: https://developer.github.com/v3/repos/releases/
[josm-plugin-list]: https://josm.openstreetmap.de/plugin
[josm-plugin-preferences]: https://josm.openstreetmap.de/wiki/Help/Preferences/Plugins
[josm-plugin-sources]: https://josm.openstreetmap.de/wiki/PluginsSource
