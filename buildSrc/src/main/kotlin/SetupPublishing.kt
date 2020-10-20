import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.credentials.AwsCredentials
import org.gradle.api.publish.PublishingExtension

/**
 * Sets up publishing to Bintray.
 * By default all artifacts that are defined in the [PublishingExtension] will be also published to Bintray.
 */
fun Project.setupBintrayPublishing() {
  System.getenv("BINTRAY_USER")?.let { bintrayUser ->
    System.getenv("BINTRAY_API_KEY")?.let { bintrayApiKey ->
      allprojects { currentProject ->
        currentProject.pluginManager.withPlugin("com.jfrog.bintray") {
          currentProject.extensions.getByType(BintrayExtension::class.java).apply {
            user = bintrayUser
            key = bintrayApiKey
            pkg.apply {
              repo = rootProject.name
              name = rootProject.name
              setLicenses("GPL-3.0-or-later")
              vcsUrl = "https://github.com/floscher/gradle-josm-plugin.git"
              githubRepo = "floscher/gradle-josm-plugin"
              issueTrackerUrl = "https://gitlab.com/floscher/gradle-josm-plugin/-/issues"
              websiteUrl = "https://floscher.gitlab.io/gradle-josm-plugin"
            }
            publish = true
            setPublications()
            // Add all publications defined for the publishing plugin
            currentProject.pluginManager.withPlugin("publishing") {
              currentProject.extensions.getByType(PublishingExtension::class.java).publications.all {
                setPublications(* (publications ?: arrayOf()).plus(it.name))
              }
            }
          }
        }
      }
    } ?: logger.lifecycle("Note: If you want to publish to Bintray, set environment variable BINTRAY_API_KEY")
  } ?: logger.lifecycle("Note: If you want to publish to Bintray, set environment variables BINTRAY_USER and BINTRAY_API_KEY")
}

/**
 * Sets up
 */
fun Project.setupBuildDirPublishingForAllProjects() {
  allprojectsPublishingRepositories {
    maven {
      it.name = "buildDir"
      it.url = uri(rootProject.buildDir.resolve("maven"))
    }
  }
}

fun Project.setupAwsPublishingForAllProjects() {
  System.getenv("AWS_ACCESS_KEY_ID")?.let { awsAccessKeyId ->
    System.getenv("AWS_SECRET_ACCESS_KEY")?.let { awsSecretAccessKey ->
      allprojectsPublishingRepositories {
        maven {
          it.url = uri("s3://gradle-josm-plugin")
          it.name = "s3"
          it.credentials(AwsCredentials::class.java) {
            it.accessKey = awsAccessKeyId
            it.secretKey = awsSecretAccessKey
          }
        }
      }
    } ?: logger.lifecycle("Note: If you want to publish to s3://gradle-josm-plugin , set the environment variable AWS_SECRET_ACCESS_KEY")
  }?: logger.lifecycle("Note: If you want to publish to s3://gradle-josm-plugin , set the environment variables AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY")
}

/**
 * Helper function that executes the given [configurationFun] on the `repositories` field of the `publishing` extension
 * in the project and all of its subprojects. If there is no `publishing` extension in on of the projects,
 * this function just does nothing.
 */
private fun Project.allprojectsPublishingRepositories(configurationFun: (RepositoryHandler).() -> Unit) {
  allprojects { p ->
    p.pluginManager.withPlugin("publishing") {
      p.extensions.getByType(PublishingExtension::class.java).repositories(configurationFun)
    }
  }
}
