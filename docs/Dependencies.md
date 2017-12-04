# Dependencies

## on JOSM plugins
If the dependencies are available as JOSM plugins, simply put the following in your `build.gradle`:
```gradle
josm.manifest {
  pluginDependencies << 'plugin1' << 'plugin2'
}
```
Replace `plugin1` and `plugin2` with the JOSM plugins you depend on, how many plugins you specify doesn't matter, it's possible to specify only one or more than the two above. You also don't need to specify transitive dependencies.

## on artifacts from Maven repositories

If your plugin has dependencies to java libraries that are not available as JOSM plugin, but via Maven, then add the following to your `build.gradle`:

```gradle
dependencies {
  packIntoJar 'groupId:artifactId:1.2.3'
  packIntoJar 'otherGroupId:otherArtifactId:42.0.0'
}
```

This will put the dependency on your classpath and packs it into your plugins \*.jar file, when building.
