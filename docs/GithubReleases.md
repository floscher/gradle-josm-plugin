# GitHub Releases for JOSM plugins

## Intro

### What are GitHub releases?

[GitHub releases][github-releases] are a way of packaging and providing software to users. GitHub provides a [REST API][github-rest-api] to manage GitHub releases.

### How does JOSM pickup a plugin from a GitHub release?
The JOSM team maintains a public directory with metadata about available plugins. It consists of a simple concatenation of the `MANIFEST`-files included in the plugin `jar`-files. You can look it up [here][josm-plugin-list]. A JOSM instance on a client computer reads downloads and parses this list. If a user decides to [install a plugin][josm-plugin-preferences], JOSM downloads the plugin `jar` from the appropriate download URL included in the plugin metadata in this list.

A plugin `MANIFEST` includes two kinds of download URLs:

1. the default download URL

   This is the download URL of the latest plugin release. Usually, it doesn't include neither a version numer nor a release label. 
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

Add a stable, release indepdent download URL for the latest release of the new plugin to [this page][josm-plugin-sources] in the JOSM wiki.

Every 10 minutes, a script in the backend infrastructure of the JOSM development team fetches the `jar`-file from this download URL, extracts the `MANIFEST` file and updates the [directory with the plugin metadata][josm-plugin-list].

## Managing GitHub releases for your plugin

The `gradle-josm-plugin` includes tasks to create GitHub releases and to upload a plugin jar as GitHub release asset.

### Two kinds of releases

The gradle plugin manages to kinds of GitHub releases:

1. a so called _pickup release_: a release with a stable, version indepenent name. The download URL for this release [can
   be registered with the JOSM backend infrastructure](#how-to-add-plugin-to-directory)

2. a list of _versioned releases_ with a release label, usually following the the conentions of [semantic versioning](https://semver.org/)

### `releases.yml` - configuration file for releases
You have to maintain a configuration file for the plugin releases.

Its default name is `releases.yml` and its default location is the project root. To configure another location

1. set the gradle property `josm.releases_config_file` either in `gradle.properties` or directly in `build.gradle`.
   ```properties
   # in gradle.properties
   josm.releases_config_file=/full/path/to/my_releases.yml
   ```
   or
   ```groovy
    //in build.gradle
    ext {
        josm {
            releases_config_file="/full/path/to/my_releases.yml"
        }
    }
   ```

Here is annotated example `releases.yml`:
```yml
# OPTIONAL: declare the label, name, and description of the pickup release. If missing, hardcoded
# default values are assumed
pickup_release_for_josm:
  label: pickup-release
  name: JOSM Pickup Release
  description: |
    This is the pickup release for the JOSM plugin system. The services
    provided by the [JOSM dev team](https://josm.openstreetmap.de)

    * download the plugin jar in this release every 10 minutes
    * extract the metadata from `META-INF/MANIFEST.INF`
    * update the metadata in the [JOSM plugin directory](https://josm.openstreetmap.de/plugin)

    ---

    {{#pickedUpReleaseLabel}}
    This release currently provides the plugin release {{pickedUpReleaseLabel}}.
    {{/pickedUpReleaseLabel}}

    {{#pickedUpReleaseDescription}}
    __Description__:
    {{pickedUpReleaseDescription}}
    {{/pickedUpReleaseDescription}}

# MANDATORY: a list of releases
#
# Entries should be ordered by label (most recent label at the top of the list) and the by
# nummeric_josm_version (highest nummeric_josm_version at the top). 
#
releases:
  # the first entry in the releases list
  - label: v0.0.2                     # MANDATORY: the release label
    numeric_josm_version: 5678        # MANDATORY: the minimal numeric josm version 
                                      # this release is  compatible with

  # the second entry in the releases list
  - label: v0.0.1                     # MANDATORY:
    numeric_josm_version: 1234        # MANDATORY:
    description: a description        # OPTIONAL
    name: a name for the release      # OPTIONAL

  # ... more entries in the releases list 
```

A minimal `releases.yml` which includes two releases `v0.0.1`and `v0.0.2` looks as follows
(note that the section for `pickup_release_for_josm` is optional):
```yml
releases:
  - label: v0.0.2
    numeric_josm_version: 5678

  - label: v0.0.1
    numeric_josm_version: 1234
```


### Create a GitHub release for your plugin ...

#### for the current build

This creates a GitHub release for the plugin version currently configured with the project property `version`.

`releases.yml` has to include a release specification for the `label` given by the project property `version`.

```yml
# releases.yml
releases:
  - label: v0.0.1
    numeric_josm_version: 1234

  # more ...
```

Create the release with the following command:
```bash
$ ./gradlew createGithubRelease
```

### for a specific label, using the command line

This creates a GitHub release with the label `v0.0.1`. Note that  `releases.yml` must include an entry for this label.

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

#### for the pickup release
Use the dedicated task `creatPickupRelease` to create the pickup release.

```bash
$ ./gradlew createPickupRelease
```

### Publish the plugin jar to a GitHub release

#### Publish to the release with the current release label

Publishes the current plugin jar as release asset to the GitHub release with the label given by the project property `version`. Note that
  * `releases.yml` must include an entry for this label.
  * an GitHub release for this label has to exist

```yml
# releases.yml
releases:
  - label: v0.0.1
    numeric_josm_version: 1234
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

#### Publish to the pickup release

Use the command line option `--publish-to-pickup-release` to publish a plugin jar not only to a normal release, but also
__to the pickup release__.

```bash
# publishes the plugin jar to the release with label 'v0.0.1' and
# to pickup release
$ ./gradlew publishToGithubRelease \
    --release-label v0.0.1 \
    --publish-to-pickup-release
```

## Configuration options

A configuration option is derived from command line arguments, properties, and environment variables in the following order:

1. command line argument
2. task configuration in `build.gradle`
3. gradle property
4. environment variable
5. hard coded default value

### common environement variables
| environment variable | description |
| --------------------- | ----------- |
| `GITHUB_USER`       | the name of the github user |
| `GITHUB_ACCESS_TOKEN`       | the Github access token |
| `GITHUB_REPOSITORY`       | the name of the github repositoy. Defaults  to the project name. |
| `GITHUB_API_URL` | the base API URL for the Github releases API. Defaults to `https://api.github.com` |
| `GITHUB_UPLOAD_URL` | the base API URL to upload release assets. Defaults to `https://uploads.github.com` |
| `GITHUB_URL`| the base GitHub URL. Defaults to `http://github.com` |

### common gradle properties
| gradle property | description |
| --------------------- | ----------- |
| `josm.github.user`       | the name of the github user |
| `josm.github.access_token`       | the Github access token |
| `josm.github.repository`       | the name of the github repositoy.  Defaults  to the project name. |
| `josm.github.api_url`  | the base API URL for the Github releases API. Defaults to `https://api.github.com` |
| `josm.github.upload_url` | the base API URL to upload release assets. Defaults to `https://uploads.github.com` |
| `josm.github.url` | the base GitHub URL. Defaults to `http://github.com` |
| `josm.releases_config_file` | the full path to the local releases file. Defaults to `releases.yml` in the base project directory | 
| `josm.target_commitish` | Specifies the commitish value that determines where the Git tag is created from. Can be any branch or commit SHA. Defaults to `master`. |


## Sample and template configuration files

### template for a script to set environment variables
```bash
#!/bin/bash
#
# save as 'github.env' and load using 'source github.env'
#

# the GitHub user name
#export GITHUB_USER=a-user-name

# the GitHub access token
#export GITHUB_ACCESS_TOKEN=asldiu0w98357oasjf

# the GitHub repository
#export GITHUB_REPOSITORY=my-repo

# the GitHub upload URL if different from https://uploads.github.com
#export GITHUB_API_URL=https://api.my-github-host.test

#  the GitHub upload URL if different from https://uploads.github.com
# export GITHUB_UPLOAD_URL=https://uploads.my-github-host.test

# the base GitHub URL. Defaults to http://github.com 
# export GITHUB_URL=http://my-github.test
```

### template for `gradle.properties`
```properties
# the GitHub user name
#josm.github.user=a-user-name

# the GitHub access token
#josm.github.access_token=asldiu0w98357oasjf

# the GitHub repository
#josm.github.repository=my-repo

# the GitHub API URL if different from https://api.github.com
#josm.github.api_url=https://api.my-github-host.test

# the GitHub upload URL if different from https://uploads.github.com
#josm.github.upload_url=https://uploads.my-github-host.test

# the base GitHub URL. Defaults to http://github.com
#josm.github.url=http://my-github.test

# the full path to the local releases config file
#josm.releases_config_file=/full/path/to/my_releases.yml

# the target commitish, if different from 'master'
#josm.target_commitish=deploy
```

### template for `build.gradle`
```groovy
plugins {
    id 'org.openstreetmap.josm'
    id 'java'
    id 'groovy'
    id 'eclipse'
}

// optional inline configuration block, or configure these properties in
// 'gradle.properties'
//ext {
//   josm {
//        github {
//            user="a-user-name"
//            access_token="asldiu0w98357oasjf"
//            repository="my-repo"
//            api_url="https://api.my-github-host.test"
//            upload_url="https://uploads.my-github-host.test"
//            url = "http://my-github.test"
//        }
//        releases_config_file="/full/path/to/my_releases.yml"
//        target_commitish="deploy"
//    }
//}

version="v0.0.5"      //  the current release label

josm {
    josmCompileVersion = "latest"
    manifest {
        // if true, the plugin will include download URLs for GitHub
        // release assets in the MANIFEST file
        includeLinksToGithubReleases = true
        //  minimal required JOSM version for the current build
        minJosmVersion = 13893
        description = 'you plugin description'
        mainClass = 'the.fully.qualified.name.of.YourPlugin'
        //iconPath = 'images/your-plugin-icon.png'
        //website = new URL("https://your.plugin.host/info-page.html")
        canLoadAtRuntime = true
    }
}

// uncomment to configure the provided task 'createGithubRelease'
/*
createGithubRelease {
    // optional. if different from the project 'version'
    //releaseLabel = "v0.0.5-GA"

    // optional. if different from 'master'
    //targetCommitish="deploy"
}
*/

// uncomment to create your own task for creating GitHub releases
/* 
import org.openstreetmap.josm.gradle.plugin.task.CreateGithubReleaseTask
task myCreateGithubReleas(task: CreateGithubReleaseTask) {
    // optional. if different from the project 'version'
    //releaseLabel = "v0.0.5-GA"

    // optional. if different from 'master'
    //targetCommitish="deploy"
}
*/


// uncomment to configure the provided task 'publishToGithubRelease'
/*
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
    
    // optional. Set to true, if the jar should be published to two
    // releases: the release with the current release label and the
    // pickup release
    //publishToPickupRelease=true
}
*/

// uncomment to create your own task for uploading to GitHub releases
/* 
import org.openstreetmap.josm.gradle.plugin.task.PublishToGithubReleaseTask
task myPublishToGithubRelease(task: PublishToGithubReleaseTask) {
    // optional. if different from the project 'version'
    //releaseLabel = "v0.0.5-GA"
    
    // optional. if different from 'master'
    //targetCommitish="deploy"

    // optional. if different from the standard path where gradle build
    // the jar
    //localJarPath="/full/path/to/the/local/my-plugin.jar"

    // optional. if different from the name of the jar file built locally
    //remoteJarName="my-plugin.jar"
    
    // optional. Set to true, if the jar should be published to two
    // releases: the release with the current release label and the
    // pickup release
    //publishToPickupRelease=true}
*/
```


[github-releases]: https://help.github.com/articles/about-releases/
[github-rest-api]: https://developer.github.com/v3/repos/releases/
[josm-plugin-list]: https://josm.openstreetmap.de/plugin
[josm-plugin-preferences]: https://josm.openstreetmap.de/wiki/Help/Preferences/Plugins
[josm-plugin-sources]: https://josm.openstreetmap.de/wiki/PluginsSource