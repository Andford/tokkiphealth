package mx.loperstack.tokkiphealth.common

/**
 * Tokkip Health's own help, in Spanish: android/docs/ of Andford/tokkiphealth. The code keeps
 * SD Maid SE's help addresses (sdmse.darken.eu/<page>#<anchor>); here each one goes to our page
 * with the same name and anchor, so nothing in the tools has to change.
 */
object TokkipDocs {
    const val BASE = "https://github.com/Andford/tokkiphealth/blob/main/android/docs/"
    private const val SDM = "https://sdmse.darken.eu"

    private val PAGES = setOf(
        "setup", "corpsefinder", "systemcleaner", "appcleaner", "appcontrol", "storageanalyzer",
        "deduplicator", "mediasqueeze", "swiper", "scheduler", "exclusions",
    )

    fun redirect(address: String): String {
        if (!address.startsWith(SDM)) return address
        val rest = address.removePrefix(SDM)
        if (rest.isNotEmpty() && rest[0] !in "/?#") return address
        val anchor = rest.substringAfter('#', "").takeIf { it.isNotEmpty() }?.let { "#$it" } ?: ""
        val page = rest.substringBefore('#').substringBefore('?').trim('/').split('/').lastOrNull { it in PAGES || it in EXTRA }
        return when (page) {
            null -> BASE + "README.md"
            "changelog" -> "https://github.com/Andford/tokkiphealth/releases"
            "privacy" -> "https://github.com/Andford/tokkiphealth/blob/main/android/PRIVACY.md"
            "bugs", "security" -> "https://github.com/Andford/tokkiphealth/issues"
            else -> "$BASE$page.md$anchor"
        }
    }

    private val EXTRA = setOf("changelog", "privacy", "bugs", "security")
}
