# GitHub Releases

## Intro

### What are GitHub releases?

[GitHub releases][github-releases] are a way of packaging and providing software to users. GitHub provides
a [REST API][github-rest-api] to manage GitHub releases.

### How does JOSM pickup a plugin from a GitHub release?
The JOSM maintainer maintain a public directory with metadata about the available
plugins. It's a simple concatenation of the `MANIFEST`-files included in the available plugin `jar`-files. It is available [here][josm-plugin-list]. A JOSM instance on a client computer reads this list. If a user decides to [install a plugin][josm-plugin-preferences], JOSM downloads the plugin `jar` from the appropriate download URL included in the metadata.

A plugin `MANIFEST` includes two kind of download URLs:

1. the default download URL
   This is the download URL of the latest plugin release. Usually, it doesn't include a version numer or release label. 
   For the `scripting` plugin, the default download URL is (you can look it up [here][josm-plugin-list])
   ```
   https://raw.githubusercontent.com/Gubaer/josm-scripting-plugin/deploy/dist/scripting.jar
   ```

2. download URLs for plugins releases compatible with JOSM releases newer than a specific reference JOSM release
   The `scripting` plugin, for instance, is available as specific releases for the JOSM versions 5315, 8024, 12875, 13007, etc.
   (you can look it up [here][josm-plugin-list]) and the `MANIFEST` includes specific download URLs for them.
   ```
   5315_Plugin-Url: 30000;https://raw.github.com/Gubaer/josm-scripting-plugin/for-josm-5315/dist/scripting.jar
   8024_Plugin-Url: 30710;https://raw.github.com/Gubaer/josm-scripting-plugin/for-josm-8024/dist/scripting.jar
   12875_Plugin-Url: 30772;https://raw.github.com/Gubaer/josm-scripting-plugin/for-josm-12875/dist/scripting.jar
   13007_Plugin-Url: 30775;https://raw.github.com/Gubaer/josm-scripting-plugin/for-josm-13007/dist/scripting.jar
   ```

### How to add a new JOSM plugin to plugin directory?

Add a stable, release indepdent download URL for the latest release of the new plugin to [this page][josm-plugin-sources] in the JOSM wiki.

Every 10 minutes, a script in the backend infrastructure of the JOSM development team fetches the `jar`-file from this download URL, extracts the `MANIFEST` file and updates the [directory with the plugin metadata][josm-plugin-list].

## Managing GitHub releases for your plugin

### `releases.yml` - configuration file for releases
You have to maintain a configuration file for the plugin releases.

Its default name is `releases.yml` and its default location is the project root. To configure another location

1. set the gradle property `josm.releases_config_file`
   either in `gradle.properties` or directly in `build.gradle`.
   ```properties
   # gradle.properties
   josm.releases_config_file=/full/path/to/my_releases.yml
   ```
   or
   ```groovy
    // build.gradle
    ext {
        josm {
            releases_config_file="/full/path/to/my_releases.yml"
        }
    }
   ```

Here is annotated example `releases.yml`:
```yml
# OPTIONAL: declare the name of the latest release. If missing, the default value is
# 'latest'.
latest_release:
  name: my_latest

# MANDATORY: a list of releases
releases:
  # the first entry in the releases list
  - label: v0.0.2  # MANDATORY: the release label
    # MANDATORY: the minimal numeric josm version this release is 
    # compatible with
    numeric_josm_version: 5678

  # the second entry in the releases list
  - label: v0.0.1
    numeric_josm_version: 1234
    # OPTIONAL: 
    description: a description
    name: a name for the release

  # ... more entries in the releases list 
```

A minimal `releases.yml` which includes two releases `v0.0.1`and `v0.0.2` looks as follows:
```yml
releases:
    label: v0.0.2
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

Publishes the current plugin jar as release asset to the GitHub release with the 
label `v0.0.1`.
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

You can configure the local path of the published `jar` and/or the remote name of the `jar`-asset
in the GitHub release.

In `build.gradle`:

```groovy
// build.gradle
publishToGithubRelease {
    localAssetPath="/path/to/my/asset/plugin.jar"
    remoteAssetName="my_other_name.jar"
}
```

TODO: check
On the command line: 
```bash
$ ./gradlew publishToGithubRelease \
    --local-asset-path /path/to/my/asset/plugin.jar \
    --remote-asset-name my_other_name.jar
```
### Configuration options

A configuration option is derived from command line arguments, properties, and environment variables in the following order:

1. command line argument
2. task configuration in `build.gradle`
3. gradle property
4. environment variable
5. hard coded default value

#### common environement variables
| environment variable | description |
| --------------------- | ----------- |
| `GITHUB_USER`       | the name of the github user |
| `GITHUB_ACCESS_TOKEN`       | the Github access token |
| `GITHUB_REPOSITORY`       | the name of the github repositoy. Defaults  to the project name. |
| `GITHUB_API_URL` | the base API URL for the Github releases API. Defaults to `https://api.github.com` |
| `GITHUB_UPLOAD_URL` | the base API URL to upload release assets. Defaults to `https://uploads.github.com` |

#### common gradle properties
| gradle property | description |
| --------------------- | ----------- |
| `josm.github.user`       | the name of the github user |
| `josm.github.access_token`       | the Github access token |
| `josm.github.repository`       | the name of the github repositoy.  Defaults  to the project name. |
| `josm.github.api_url`  | the base API URL for the Github releases API. Defaults to `https://api.github.com` |
| `josm.github.upload_url` | the base API URL to upload release assets. Defaults to `https://uploads.github.com` |
| `josm.releases_config_file` | the full path to the local releases file. Defaults to `releases.yml` in the base project directory | 
| `josm.target_commitish` | Specifies the commitish value that determines where the Git tag is created from. Can be any branch or commit SHA. Defaults to `master`. |


[github-releases]: https://help.github.com/articles/about-releases/
[github-rest-api]: https://developer.github.com/v3/repos/releases/
[josm-plugin-list]: https://josm.openstreetmap.de/plugin
[josm-plugin-preferences]: https://josm.openstreetmap.de/wiki/Help/Preferences/Plugins
[josm-plugin-sources]: https://josm.openstreetmap.de/wiki/PluginsSource