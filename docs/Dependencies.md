# Dependencies

If your plugin has dependencies to java libraries that are not available as JOSM plugin, but via Maven, then add the following to your `build.gradle`:

```gradle
dependencies {
  packIntoJar 'groupId:artifactId:1.2.3'
  packIntoJar 'otherGroupId:otherArtifactId:42.0.0'
}
```

This will put the dependency on your classpath and packs it into your plugins \*.jar file, when building.
