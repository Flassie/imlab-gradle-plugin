package io.flassie.imlab

data class GitlabPublishing(
    var enable: Boolean = false,
    var projectId: Int = gitlabProjectId ?: -1,
    var allowNonCI: Boolean = false,
    var privateKeyVariable: String = "imlabGitlabKey",
    var version: String? = null
)