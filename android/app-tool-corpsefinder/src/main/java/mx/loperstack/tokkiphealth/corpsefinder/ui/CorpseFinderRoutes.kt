package mx.loperstack.tokkiphealth.corpsefinder.ui

import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.navigation.NavigationDestination
import mx.loperstack.tokkiphealth.common.serialization.APathSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

@Serializable
data object CorpseFinderSettingsRoute : NavigationDestination

@Serializable
data object CorpseFinderListRoute : NavigationDestination

@Serializable
data class CorpseDetailsRoute(
    val corpsePathJson: String? = null,
) : NavigationDestination {
    constructor(corpsePath: APath?) : this(
        corpsePathJson = corpsePath?.let { Json.encodeToString(APathSerializer, it) },
    )

    @Transient
    val corpsePath: APath? = corpsePathJson?.let {
        // Defensive: a malformed/legacy-schema JSON string must not crash the nav framework at
        // route construction. The VM falls back to lastPosition when corpsePath is null.
        runCatching { Json.decodeFromString(APathSerializer, it) }.getOrNull()
    }
}
