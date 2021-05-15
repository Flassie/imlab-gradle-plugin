package io.flassie.imlab

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.SelfResolvingDependency
import org.gradle.api.attributes.Category
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*

@Suppress("unused", "SpellCheckingInspection")
class ImlabPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create<ImlabPluginExtension>("imlab")

        target.afterEvaluate {
            val gitlabPublishing = extension.gitlabPublishing

            if (gitlabPublishing != null) {
                target.plugins.apply(MavenPublishPlugin::class.java)
            }

            if (gitlabPublishing != null && gitlabPublishing.enable) {
                target.afterEvaluate {
                    registerPublishing(target, extension)
                }
            }
        }
    }

    private fun registerPublishing(project: Project, extension: ImlabPluginExtension) {
        val gitlabPublishing = extension.gitlabPublishing!!

        with(project) {
            val ciToken = System.getenv("CI_JOB_TOKEN")
            if(gitlabPublishing.allowNonCI || ciToken != null) {
                configure<PublishingExtension> {
                    repositories {
                        gitlab(
                            projectId = gitlabPublishing.projectId,
                            privateKey = project.rootProject.properties[gitlabPublishing.privateKeyVariable]?.toString()
                        )
                    }

                    publications {
                        create<MavenPublication>("gitlabPublication") {
                            generatePomDependencies(project)

                            from(components["java"])

                            val sourcesJar by tasks.creating(Jar::class) {
                                archiveClassifier.set("sources")
                                from(project.sourceSets.getByName("main").allSource)
                            }

                            artifact(sourcesJar)
                        }
                    }
                }
            }
        }
    }

    private fun MavenPublication.generatePomDependencies(project: Project) {
        pom {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            withXml {
                val node = asNode()
                val dependenciesNode = node.appendNode("dependencies")

                val visited = hashSetOf<Dependency>()

                project.handleDependencies(
                    listOf("api"),
                    "compile",
                    dependenciesNode,
                    visited
                )

                project.handleDependencies(
                    listOf("implementation", "runtimeOnly"),
                    "runtime",
                    dependenciesNode,
                    visited
                )
            }
        }
    }

    private fun Project.handleDependencies(
        configurations: List<String>,
        scope: String,
        dependenciesNode: groovy.util.Node,
        visited: MutableSet<Dependency>
    ) {
        configurations.forEach {
            this.configurations[it].allDependencies.forEach configsForEach@ { dep ->
                if(visited.contains(dep)) return@configsForEach

                handleDependency(dep, dependenciesNode, scope)
                visited.add(dep)
            }
        }
    }

    private fun handleDependency(dependency: Dependency, node: groovy.util.Node, scope: String) {
        if(dependency is ModuleDependency) {
            val categoryAttribute = dependency.attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)
            val isPlatform = categoryAttribute?.name == Category.REGULAR_PLATFORM || categoryAttribute?.name == Category.ENFORCED_PLATFORM

            if(isPlatform) return
        }

        if(dependency is ProjectDependency) {
            val group = dependency.dependencyProject.group.toString()
            val name = dependency.dependencyProject.name
            val version = dependency.dependencyProject.version

            node.appendNode("dependency").apply {
                appendNode("groupId", group)
                appendNode("artifactId", name)

                appendNode("version", version)
                appendNode("scope", scope)
            }
        } else if(dependency !is SelfResolvingDependency) {
            node.appendNode("dependency").apply {
                appendNode("groupId", dependency.group)
                appendNode("artifactId", dependency.name)

                if(dependency.version != null)
                    appendNode("version", dependency.version)

                appendNode("scope", scope)
            }
        }
    }

    val Project.sourceSets: SourceSetContainer get() =
        (this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer

}