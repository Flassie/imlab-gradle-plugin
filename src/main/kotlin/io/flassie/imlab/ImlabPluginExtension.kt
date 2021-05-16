package io.flassie.imlab

open class ImlabPluginExtension {
    var privateKeyVariable: String = "imlabGitlabKey"
    var gitlabPublishing: GitlabPublishing? = null

    fun gitlabPublishing(builder: GitlabPublishing.() -> Unit) {
        val newVersion = GitlabPublishing()
        newVersion.builder()

        gitlabPublishing = newVersion
    }
}