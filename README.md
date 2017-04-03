# Gradle tasks for running JOSM

## Getting started
### Prerequisites
You naturally need to build your project (JOSM core or a JOSM plugin) using Gradle and use the Java plugin provided by Gradle.

### JOSM core

Put the files `run-josm-tasks.gradle` and `run-josm-config.gradle` into the `./gradle/` directory of your repository.

Add the following line to your `./build.gradle` file after the `sourceSet{}` block:
```gradle
apply from: 'gradle/run-josm-tasks.gradle'
```

### JOSM plugins

Put all three files `run-josm-tasks.gradle`, `run-josm-plugin-tasks.gradle` and `run-josm-config.gradle` into the `./gradle/` directory of your repository.

Add the following line to your `./build.gradle` file after the `sourceSet{}` block:
```gradle
apply from: 'gradle/run-josm-plugin-tasks.gradle'
```

### Configuration
You can modify the configuration in the file `run-josm-config.gradle` to your choosing.

## Projects using these Gradle tasks
* [JOSM/Mapillary](https://github.com/JOSM/Mapillary)
* [floscher/josm](https://github.com/floscher/josm)
