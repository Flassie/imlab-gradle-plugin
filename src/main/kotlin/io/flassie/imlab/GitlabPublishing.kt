package io.flassie.imlab

data class GitlabPublishing(
    val projectId: Int,
    val allowNonCI: Boolean = false,
    val privateKeyVariable: String = "imlabGitlabKey"
)