import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.SelfResolvingDependency
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.attributes.Category
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.*

@Suppress("unused", "SpellCheckingInspection")
class ImlabPlugin : Plugin<Project> {
    companion object {
        const val NEXUS_USERNAME_PROP = "nexusUsername"
        const val NEXUS_PASSWORD_PROP = "nexusPassword"
        const val NEXUS_URL = "https://nexus.int.imlab.by/repository"
    }

    override fun apply(target: Project) {
        val extension = target.extensions.create<ImlabPluginExtension>("imlab")

        target.afterEvaluate {
            extension.repositories.forEach {
                repositories {
                    maven {
                        name = it
                        url = uri("$NEXUS_URL/$it")

                        if(!it.endsWith("-public")) {
                            credentials {
                                username = project.rootProject.extra[NEXUS_USERNAME_PROP].toString()
                                password = project.rootProject.extra[NEXUS_PASSWORD_PROP].toString()
                            }
                        }

                        println("Added imlab repository: $url")
                    }
                }
            }
        }

        if(extension.enablePublish) {
            target.plugins.apply(MavenPublishPlugin::class.java)

            target.afterEvaluate {
                registerPublishing(target)
            }
        }
    }

    private fun registerPublishing(project: Project) {
        with(project) {
            configure<PublishingExtension> {
                repositories {
                    maven {
                        url = uri(NEXUS_URL)

                        credentials {
                            username = project.rootProject.extra[NEXUS_USERNAME_PROP].toString()
                            password = project.rootProject.extra[NEXUS_PASSWORD_PROP].toString()
                        }
                    }
                }

                publications {
                    create<MavenPublication>("imlabNexusPublication")
                        .generatePomDependencies(project)
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
            println(dependency)
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
            println(dependency)
            node.appendNode("dependency").apply {
                appendNode("groupId", dependency.group)
                appendNode("artifactId", dependency.name)

                if(dependency.version != null)
                    appendNode("version", dependency.version)

                appendNode("scope", scope)
            }
        }
    }
}