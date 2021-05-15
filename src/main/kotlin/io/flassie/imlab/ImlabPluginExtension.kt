package io.flassie.imlab

open class ImlabPluginExtension {
    var gitlabPublishing: GitlabPublishing? = null

    fun gitlabPublishing(builder: GitlabPublishing.() -> Unit) {
        GitlabPublishing().builder()
    }
}