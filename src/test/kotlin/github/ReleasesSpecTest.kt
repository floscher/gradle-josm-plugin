package org.openstreetmap.josm.gradle.plugin.github
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReleasesSpecTest {

  @Test
  fun `should read a yaml file with local releases`() {
    val releasesStream = """
      releases:
        - label: v0.0.2
          name: a name 1
          minJosmVersion: 1234
          description: a description

        - label: v0.0.1
          name: a name 2
          minJosmVersion: 1234
          description: a description
      """.trimIndent().byteInputStream()

    val releases = ReleaseSpec.loadListFrom(releasesStream)

    assertEquals(2, releases.size)
    assertEquals("v0.0.2", releases[0].label)
    assertEquals(1234, releases[0].minJosmVersion)
    assertEquals("a description", releases[0].description)
    assertEquals("a name 1", releases[0].name)
  }

  @Test
  fun `a release with a description and a name should be accepted`() {
    val releasesStream = """
      releases:
        - label: v0.0.1
          minJosmVersion: 8293
          name: ä_name
          description: ä_description
      """.trimIndent().byteInputStream()
    val releases = ReleaseSpec.loadListFrom(releasesStream)
    assertEquals(1, releases.size)
    assertEquals("ä_description", releases[0].description)
    assertEquals("ä_name", releases[0].name)
    assertEquals("v0.0.1", releases[0].label)
    assertEquals(8293, releases[0].minJosmVersion)
  }

  @Test
  fun `a release with a missing description and a missing name should be accepted`() {
    val releasesStream = """
      releases:
        - label: v0.0.1
          minJosmVersion: 987654321
      """.trimIndent().byteInputStream()

    val releases = ReleaseSpec.loadListFrom(releasesStream)
    assertEquals(1, releases.size)
    assertNull(releases[0].description)
    assertEquals("v0.0.1", releases[0].label)
    assertEquals("Release v0.0.1", releases[0].name)
    assertEquals(987654321, releases[0].minJosmVersion)
  }

  @Test
  fun `should read a yaml file with an empty list of releases`() {
    val releasesStream = "releases:\n\r\n".byteInputStream()
    val releases = ReleaseSpec.loadListFrom(releasesStream)
    assertEquals(0, releases.size)
    assertEquals(0, releases.onlyFallbackVersions().size)
  }

  @Test
  fun `relevant releases - exactly one release`() {
    val releasesStream = """
      releases:
        - label: 0.0.1
          minJosmVersion: 1234
          description: A short description
      """.trimIndent().byteInputStream()

    val fallback = ReleaseSpec.loadListFrom(releasesStream).onlyFallbackVersions()
    assertEquals(1, fallback.size)
    assertEquals("0.0.1", fallback[0].label)
    assertEquals(1234, fallback[0].minJosmVersion)
    assertEquals("A short description", fallback[0].description)
    assertEquals("Release 0.0.1", fallback[0].name)
  }

  @Test
  fun `fallback releases - multiple releases, same josm version`() {
    val releasesStream = """
      releases:
      - label: 0.0.3
        minJosmVersion: 1234
      - label: 0.0.2
        minJosmVersion: 1234
      - label: 0.0.1
        minJosmVersion: 1234
      """.trimIndent().byteInputStream()
    val releases = ReleaseSpec.loadListFrom(releasesStream)
    val fallback = releases.onlyFallbackVersions()
    assertEquals(3, releases.size)
    assertEquals(1, fallback.size)
    assertEquals(fallback[0].label, "0.0.3")
    assertEquals(fallback[0].minJosmVersion, 1234)
  }

  @Test
  fun `fallback releases - multiple releases, multiple josm version`() {
    val releasesStream = """
      releases:
        - label: 0.0.4
          minJosmVersion: 2
        - label: 0.0.3
          minJosmVersion: 2
        - label: 0.0.2
          minJosmVersion: 1
        - label: 0.0.1
          minJosmVersion: 1
      """.trimIndent().byteInputStream()
    val releases = ReleaseSpec.loadListFrom(releasesStream)
    val fallback = releases.onlyFallbackVersions()
    assertEquals(4, releases.size)
    assertIterableEquals(listOf("0.0.4", "0.0.3", "0.0.2", "0.0.1"), releases.map { it.label })
    assertIterableEquals(listOf(2, 2, 1, 1), releases.map { it.minJosmVersion })
    assertEquals(2, fallback.size)
    assertEquals("0.0.4", fallback[0].label)
    assertEquals(2, fallback[0].minJosmVersion)
    assertEquals("0.0.2", fallback[1].label)
    assertEquals(1, fallback[1].minJosmVersion)
  }
}

