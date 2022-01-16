import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.credentials.AwsCredentials
import org.gradle.api.publish.PublishingExtension

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
