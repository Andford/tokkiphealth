package mx.loperstack.tokkiphealth.deduplicator.ui

import mx.loperstack.tokkiphealth.common.navigation.NavigationDestination
import mx.loperstack.tokkiphealth.deduplicator.core.Duplicate
import kotlinx.serialization.Serializable

@Serializable
data object DeduplicatorSettingsRoute : NavigationDestination

@Serializable
data object DeduplicatorListRoute : NavigationDestination

@Serializable
data class DeduplicatorDetailsRoute(
    val identifier: Duplicate.Cluster.Id? = null,
) : NavigationDestination

@Serializable
data object ArbiterConfigRoute : NavigationDestination
