import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

public fun Project.configureIfMultiplatform(config: (KotlinMultiplatformExtension) -> Unit): Unit =
  configureGradlePlugin<KotlinMultiplatformPluginWrapper, KotlinMultiplatformExtension>(config)

public fun Project.configureIfJvm(config: (KotlinJvmProjectExtension) -> Unit): Unit =
  configureGradlePlugin<KotlinPluginWrapper, KotlinJvmProjectExtension>(config)

public fun Project.configureIfMavenPublishing(config: (PublishingExtension) -> Unit): Unit =
  configureGradlePlugin<MavenPublishPlugin, PublishingExtension>(config)

public fun Project.configureIfJacoco(config: (JacocoPluginExtension) -> Unit): Unit =
  configureGradlePlugin<JacocoPlugin, JacocoPluginExtension>(config)

private inline fun <reified P: Plugin<Project>, reified E> Project.configureGradlePlugin(crossinline config: (E) -> Unit) {
  plugins.withType(P::class.java).whenPluginAdded {
    config(extensions.getByType(E::class.java))
  }
}
