
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.openstreetmap.josm.gradle.plugin.task.ReleasesSpec

class ReleasesSpecTest {

  @Test
  fun `should read a yaml file with local releases`() {
    val releasesFile = createTempFile(suffix="yml")
    val releases = """
        releases:
          - label: v0.0.2
            name: a name 1
            numeric_plugin_version: 1
            numeric_josm_version: 1234
            description: a description

          - label: v0.0.1
            name: a name 2
            numeric_plugin_version: 1
            numeric_josm_version: 1234
            description: a description
     """.trimIndent()
    try {
      releasesFile.writeText(releases)
      val releases = ReleasesSpec.load(releasesFile)?.releases
        ?: throw Exception("no releases")

      assertEquals(releases.size, 2)
      assertEquals(releases[0].label, "v0.0.2")
      assertEquals(releases[0].numericJosmVersion, 1234)
      assertEquals(releases[0].numericPluginVersion, 1)
      assertEquals(releases[0].description, "a description")
      assertEquals(releases[0].name, "a name 1")
    } finally {
        releasesFile.delete()
    }
  }

  @Test
  fun `a release with a description and a name should be accepted`() {
    val releasesFile = createTempFile(suffix="yml")
    val releases = """
        releases:
          - label: v0.0.1
            numeric_plugin_version: 1
            numeric_josm_version: 1234
            name: a_name
            description: a_description
     """.trimIndent()
    try {
      releasesFile.writeText(releases)
      val releases = ReleasesSpec.load(releasesFile)?.releases
        ?: throw Exception("no releases")
      assertEquals(releases.size, 1)
      assertEquals(releases[0].description, "a_description")
      assertEquals(releases[0].name, "a_name")
    } finally {
      releasesFile.delete()
    }
  }

  @Test
  fun `a release with a missing description and a missing name should be accepted`() {
    val releasesFile = createTempFile(suffix="yml")
    val releases = """
        releases:
          - label: v0.0.1
            numeric_plugin_version: 1
            numeric_josm_version: 1234
     """.trimIndent()
    try {
      releasesFile.writeText(releases)
      val releases = ReleasesSpec.load(releasesFile)?.releases
        ?: throw Exception("no releases")
      assertEquals(releases.size, 1)
      assertNull(releases[0].description)
      assertEquals(releases[0].name, releases[0].label)
    } finally {
      releasesFile.delete()
    }
  }

  @Test
  fun `should read a yaml file with an empty list of releases`() {
    val releasesFile = createTempFile(suffix="yml")
    val releases = """
        releases:
     """.trimIndent()
    try {
      releasesFile.writeText(releases)
      val releases = ReleasesSpec.load(releasesFile)?.releases
        ?: throw Exception("no releases")
      assertEquals(releases.size, 0)
    } finally {
      releasesFile.delete()
    }
  }
}

