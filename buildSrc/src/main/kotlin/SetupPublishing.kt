import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.credentials.AwsCredentials
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import java.io.File

/**
 * Adds a Maven repo in the directory `./maven` inside the [Project.getBuildDir] of the [Project.getRootProject]
 * to which artifacts can be published.
 */
public fun Project.setupBuildDirPublishing() {
  addPublishingRepositories {
    maven {
      it.name = "buildDir"
      it.url = uri(rootProject.buildDir.resolve("maven"))
    }
  }
}

public fun Project.setupAwsPublishing() {
  addPublishingRepositories {
    val repository = project.providers.environmentVariable("AWS_ACCESS_KEY_ID").forUseAtConfigurationTime().orNull?.let { awsAccessKeyId ->
      project.providers.environmentVariable("AWS_SECRET_ACCESS_KEY").forUseAtConfigurationTime().orNull?.let { awsSecretAccessKey ->
        maven {
          it.url = project.uri("s3://gradle-josm-plugin")
          it.name = "s3"
          it.credentials(AwsCredentials::class.java) {
            it.accessKey = awsAccessKeyId
            it.secretKey = awsSecretAccessKey
          }
        }
      }
    }
    if (repository == null) {
      logger.lifecycle(
        "Note: Set the environment variables AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY in order to publish to s3://gradle-josm-plugin ."
      )
    }
  }
}

public fun Project.setupOssSonatypeStagingPublishing() {
  addPublishingRepositories {
    providers.environmentVariable("SONATYPE_USERNAME").forUseAtConfigurationTime().orNull?.let { sonatypeUsername ->
      providers.environmentVariable("SONATYPE_PASSWORD").forUseAtConfigurationTime().orNull?.let { sonatypePassword ->
        maven {
          it.name = "OssSonatypeStaging"
          it.url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
          it.credentials {
            it.username = sonatypeUsername
            it.password = sonatypePassword
          }
        }
      }
    } ?: logger.lifecycle("Note: Set the environment variables SONATYPE_USERNAME and SONATYPE_PASSWORD in order to publish to https://oss.sonatype.org/content/groups/staging/")
  }
}

/**
 * Helper function that executes the given [configurationFun] on the `repositories` field of the `publishing` extension
 * in the project and all of its subprojects. If there is no `publishing` extension in on of the projects,
 * this function just does nothing.
 */
private fun Project.addPublishingRepositories(conf: (RepositoryHandler).() -> Unit) {
  plugins.withType(PublishingPlugin::class).whenPluginAdded {
    extensions.getByType(PublishingExtension::class).repositories(conf)
  }
}

/**
 * This can be passed e.g. to [Project.subprojects] or [Project.allprojects] to setup signing for all
 * [PublishToMavenRepository] tasks in the project.
 * The path to the private PGP key file is read from the environment variable `SIGNING_PGP_PRIVATE_KEY_PATH`,
 * the passphrase for it is read from `SIGNING_PGP_PASSWORD`.
 * @return a consumer function that sets up the [SigningPlugin] for projects that are passed as argument.
 */
public fun Project.setupMavenArtifactSigning() {
  plugins.withType(MavenPublishPlugin::class).whenPluginAdded {
    providers.environmentVariable("SIGNING_PGP_PRIVATE_KEY_PATH").forUseAtConfigurationTime().orNull
      ?.let { File(it) }
      ?.takeIf { it.canRead() }
      ?.readText()
      ?.let { privateKey ->
        apply<SigningPlugin>()
        extensions.findByType<SigningExtension>()!!.let { signingExtension -> // see above: Signing plugin is applied
          signingExtension.useInMemoryPgpKeys(
            privateKey,
            providers.environmentVariable("SIGNING_PGP_PASSWORD").forUseAtConfigurationTime().getOrElse("")
          )
          extensions.findByType<PublishingExtension>()!!.let { publishingExtension -> // see above: publishing plugin is applied
            publishingExtension.publications.withType<MavenPublication>().all { signingExtension.sign(it) }
          }
        }
      }
      ?: logger.lifecycle(
        "Note: Set the environment variable SIGNING_PGP_PRIVATE_KEY_PATH to the file path of a PGP private key "
        + "in order to your maven artifacts will be signed."
      )
  }
}

public fun Project.addMavenPomContent(pomContent: (MavenPom).() -> Unit) {
  plugins.withType(MavenPublishPlugin::class).whenPluginAdded {
    extensions.getByType(PublishingExtension::class).publications.withType(MavenPublication::class).all {
      it.pom(pomContent)
    }
  }
}
