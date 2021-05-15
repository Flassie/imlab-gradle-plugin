package io.flassie.imlab

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getByType
import java.net.URI

const val GITLAB_HOST="gitlab.imlab.by"

val gitTag: String?
    get() = System.getenv("CI_COMMIT_TAG")

val gitTagVersion: String?
    get() = gitTag?.removePrefix("v")

val gitlabProjectId: Int?
    get() = System.getenv("CI_PROJECT_ID")?.toInt()

fun getGitlabRepositoryURL(groupId: Int? = null, projectId: Int? = null): URI {
    if(groupId == null && projectId == null) throw RuntimeException("groupId or projectId must be specified")
    if(groupId != null && projectId != null) throw RuntimeException("Both groupId and projectId can't be specified")

    return if(groupId != null) {
        URI("https://$GITLAB_HOST/api/v4/groups/$groupId/-/packages/maven")
    } else {
        URI("https://$GITLAB_HOST/api/v4/projects/$projectId/packages/maven")
    }
}

val Project.gitlabKey: String?
    get() {
        val ciToken = System.getenv("CI_JOB_TOKEN")
        return if(ciToken != null) {
            ciToken
        } else {
            val pkVariable = this.extensions.getByType<ImlabPluginExtension>().gitlabPublishing?.privateKeyVariable
            if(pkVariable != null) {
                project.rootProject.properties[pkVariable]?.toString()
            } else null
        }
    }

fun MavenArtifactRepository.setupGitlabAuthentication(default: String?) {
    val ciToken = System.getenv("CI_JOB_TOKEN")

    if(ciToken == null && default == null) return

    credentials(HttpHeaderCredentials::class.java) {
        if(ciToken != null) {
            name = "Job-Token"
            value = ciToken
        } else if(default != null) {
            name = "Private-Token"
            value = default
        }
    }

    authentication {
        create<HttpHeaderAuthentication>("httpHeader")
    }
}

fun RepositoryHandler.gitlab(groupId: Int? = null, projectId: Int? = null, privateKey: String? = null) {
    val repositoryUrl = getGitlabRepositoryURL(groupId, projectId)

    maven {
        url = repositoryUrl
        setupGitlabAuthentication(privateKey)
    }
}