package io.flassie.imlab

data class GitlabPublishing(
    var enable: Boolean = false,
    var projectId: Int? = gitlabProjectId,
    var allowNonCI: Boolean = false,
    var version: String? = null
)