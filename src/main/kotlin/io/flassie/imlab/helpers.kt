package io.flassie.imlab

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import java.net.URI

const val GITLAB_HOST="gitlab.imlab.by"

fun getGitlabRepositoryURL(groupId: Int? = null, projectId: Int? = null): URI {
    if(groupId == null && projectId == null) throw RuntimeException("groupId or projectId must be specified")
    if(groupId != null && projectId != null) throw RuntimeException("Both groupId and projectId can't be specified")

    return if(groupId != null) {
        URI("https://gitlab.imlab.by/api/v4/groups/$groupId/-/packages/maven")
    } else {
        URI("https://gitlab.imlab.by/api/v4/projects/$projectId/packages/maven")
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
                project.rootProject.extra[pkVariable].toString()
            } else null
        }
    }

fun MavenArtifactRepository.setupGitlabAuthentication(default: String) {
    credentials(HttpHeaderCredentials::class.java) {
        val ciToken = System.getenv("CI_JOB_TOKEN")
        if(ciToken == null) {
            name = "Private-Token"
            value = default
        } else {
            name = "Job-Token"
            value = ciToken
        }
    }

    authentication {
        create<HttpHeaderAuthentication>("httpHeader")
    }
}

fun RepositoryHandler.gitlab(groupId: Int? = null, projectId: Int? = null, privateKey: String) {
    val repositoryUrl = getGitlabRepositoryURL(groupId, projectId)

    maven {
        url = repositoryUrl
        setupGitlabAuthentication(privateKey)
    }
}