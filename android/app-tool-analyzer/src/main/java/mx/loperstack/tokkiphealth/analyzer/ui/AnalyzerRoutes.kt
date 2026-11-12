package mx.loperstack.tokkiphealth.analyzer.ui

import mx.loperstack.tokkiphealth.analyzer.core.content.ContentGroup
import mx.loperstack.tokkiphealth.common.navigation.NavigationDestination
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import mx.loperstack.tokkiphealth.common.storage.StorageId
import kotlinx.serialization.Serializable

@Serializable
data object AnalyzerSettingsRoute : NavigationDestination

@Serializable
data class StorageContentRoute(
    val storageId: StorageId,
) : NavigationDestination

@Serializable
data class AppsRoute(
    val storageId: StorageId,
) : NavigationDestination

@Serializable
data class AppDetailsRoute(
    val storageId: StorageId,
    val installId: InstallId,
) : NavigationDestination

@Serializable
data class ContentRoute(
    val storageId: StorageId,
    val groupId: ContentGroup.Id,
    val installId: InstallId? = null,
) : NavigationDestination
