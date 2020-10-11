@Suppress("unused", "SpellCheckingInspection")
open class ImlabPluginExtension {
    var enablePublish = true
    var repositories = mutableListOf<String>()

    fun ikassaRepository(name: String, isPublic: Boolean = false) {
        repositories.add(name.getFullName("ikassa", isPublic))
    }

    fun imlabRepository(name: String, isPublic: Boolean = false) {
        repositories.add(name.getFullName("imlab", isPublic))
    }

    fun repository(name: String, isPublic: Boolean = false) {
        repositories.add(name.getFullName(isPublic))
    }

    private fun String.getFullName(isPublic: Boolean): String = getFullName("", isPublic)

    private fun String.getFullName(prefix: String, isPublic: Boolean): String {
        val builder = StringBuilder()

        if(prefix.isNotBlank())
            builder.append("$prefix-")

        builder.append(this)

        if(isPublic)
            builder.append("-public")

        return builder.toString()
    }
}