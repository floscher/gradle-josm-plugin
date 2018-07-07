
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.openstreetmap.josm.gradle.plugin.task.DEFAULT_LATEST_LABEL
import org.openstreetmap.josm.gradle.plugin.task.ReleasesSpec

class ReleasesSpecTest {

    @Test
    fun `should read a yaml file with local releases`() {
        val releasesFile = createTempFile(suffix="yml")
        val releases = """
            releases:
              - label: v0.0.2
                name: a name 1
                numeric_josm_version: 1234
                description: a description

              - label: v0.0.1
                name: a name 2
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

    @Test
    fun `one josm version in the releases yml`() {
        val releasesFile = createTempFile(suffix="yml")
        val releases = """
        releases:
          - label: v0.0.1
            numeric_josm_version: 1234
     """.trimIndent()
        try {
            releasesFile.writeText(releases)
            val releases = ReleasesSpec.load(releasesFile)
            val josmVersions = releases?.josmVersions()?.sorted()
            assertEquals(josmVersions, listOf(1234))
        } finally {
            releasesFile.delete()
        }
    }

    @Test
    fun `multiple josm versions in the releases yml`() {
        val releasesFile = createTempFile(suffix="yml")
        val releases = """
        releases:
          - label: v0.0.3
            numeric_josm_version: 3
          - label: v0.0.2
            numeric_josm_version: 2
          - label: v0.0.1
            numeric_josm_version: 1
     """.trimIndent()
        try {
            releasesFile.writeText(releases)
            val releases = ReleasesSpec.load(releasesFile)
            val josmVersions = releases?.josmVersions()?.sorted()
            assertEquals(josmVersions, listOf(1,2,3))
        } finally {
            releasesFile.delete()
        }
    }

    @Test
    fun `no josm versions in the releases file`() {
        val releasesFile = createTempFile(suffix="yml")
        val releases = """
        releases:
     """.trimIndent()
        try {
            releasesFile.writeText(releases)
            val releases = ReleasesSpec.load(releasesFile)
            val josmVersions = releases?.josmVersions()?.sorted()
            assertEquals(josmVersions, emptyList<Int>())
        } finally {
            releasesFile.delete()
        }
    }

    @Test
    fun `default latest release, if not specified in file`() {
        val releasesFile = createTempFile(suffix="yml")
        val releases = """
        releases:
     """.trimIndent()
        try {
            releasesFile.writeText(releases)
            val releases = ReleasesSpec.load(releasesFile)
            assertEquals(DEFAULT_LATEST_LABEL, releases?.latestLabel)
            assertEquals(DEFAULT_LATEST_LABEL, releases?.latestRelease?.label)
        } finally {
            releasesFile.delete()
        }
    }

    @Test
    fun `should accept customized latest release label`() {
        val releasesFile = createTempFile(suffix="yml")
        val releases = """
        latest_release:
          label: current

        releases:
     """.trimIndent()
        try {
            releasesFile.writeText(releases)
            val releases = ReleasesSpec.load(releasesFile)
            assertEquals("current", releases?.latestLabel)
            assertEquals("current", releases?.latestRelease?.label)
        } finally {
            releasesFile.delete()
        }
    }


    @Test
    fun `relevant releases - exactly one release`() {
        val releasesFile = createTempFile(suffix="yml")
        val releases = """
        releases:
          - label: v0.0.1
            numeric_josm_version: 1234
     """.trimIndent()
        try {
            releasesFile.writeText(releases)
            val releases = ReleasesSpec.load(releasesFile)!!
            val relevant = releases.relevantReleasesForDownloadUrls()
            assertEquals(relevant.size,1)
            assertEquals(relevant.first()!!.label, "v0.0.1")
        } finally {
            releasesFile.delete()
        }
    }

    @Test
    fun `relevant releases - multiple releases, same josm version`() {
        val releasesFile = createTempFile(suffix="yml")
        val releases = """
        releases:
          - label: v0.0.3
            numeric_josm_version: 1234
          - label: v0.0.2
            numeric_josm_version: 1234
          - label: v0.0.1
            numeric_josm_version: 1234
     """.trimIndent()
        try {
            releasesFile.writeText(releases)
            val releases = ReleasesSpec.load(releasesFile)!!
            val relevant = releases.relevantReleasesForDownloadUrls()
            assertEquals(relevant.size,1)
            assertEquals(relevant[0].label, "v0.0.3")
            assertEquals(relevant[0].numericJosmVersion, 1234)
        } finally {
            releasesFile.delete()
        }
    }

    @Test
    fun `relevant releases - multiple releases, multiple josm version`() {
        val releasesFile = createTempFile(suffix="yml")
        val releases = """
        releases:
          - label: v0.0.4
            numeric_josm_version: 2
          - label: v0.0.3
            numeric_josm_version: 2
          - label: v0.0.2
            numeric_josm_version: 1
          - label: v0.0.1
            numeric_josm_version: 1
     """.trimIndent()
        try {
            releasesFile.writeText(releases)
            val releases = ReleasesSpec.load(releasesFile)!!
            val relevant = releases.relevantReleasesForDownloadUrls()
            assertEquals(relevant.size,2)
            assertEquals(relevant[0].label, "v0.0.2")
            assertEquals(relevant[0].numericJosmVersion, 1)
            assertEquals(relevant[1].label, "v0.0.4")
            assertEquals(relevant[1].numericJosmVersion, 2)
        } finally {
            releasesFile.delete()
        }
    }
}

