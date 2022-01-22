# Dependencies

## on JOSM plugins
If the dependencies are available as JOSM plugins, simply put the following in your `build.gradle`:
```kotlin
josm.manifest {
  pluginDependencies += setOf("plugin1", "plugin2")
}
```
Replace `plugin1` and `plugin2` with the JOSM plugins you depend on, how many plugins you specify doesn't matter, it's possible to specify only one or more than the two above. You also don't need to specify transitive dependencies.

## on artifacts from Maven repositories

If your plugin has dependencies to java libraries that are not available as JOSM plugin, but via Maven, then add the following to your `build.gradle`:

```kotlin
repositories {
  // Define any additional repositories here (see https://docs.gradle.org/6.8.1/userguide/declaring_repositories.html)
  // For the repositories that are automatically available with the gradle-josm-plugin, see https://josm.gitlab.io/gradle-josm-plugin/kdoc/latest/plugin/plugin/org.openstreetmap.josm.gradle.plugin.config/-josm-plugin-extension/index.html#org.openstreetmap.josm.gradle.plugin.config/JosmPluginExtension/repositories/#/PointingToDeclaration/
}
dependencies {
  packIntoJar("groupId:artifactId:1.2.3")
  packIntoJar("otherGroupId:otherArtifactId:42.0.0")
}
```

This will put the dependency on your classpath and packs it into your plugins \*.jar file, when building.
