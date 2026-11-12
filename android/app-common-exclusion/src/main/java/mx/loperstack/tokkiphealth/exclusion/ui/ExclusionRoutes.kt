package mx.loperstack.tokkiphealth.exclusion.ui

import mx.loperstack.tokkiphealth.common.navigation.NavigationDestination
import mx.loperstack.tokkiphealth.exclusion.ui.editor.path.PathExclusionEditorOptions
import mx.loperstack.tokkiphealth.exclusion.ui.editor.pkg.PkgExclusionEditorOptions
import mx.loperstack.tokkiphealth.exclusion.ui.editor.segment.SegmentExclusionEditorOptions
import kotlinx.serialization.Serializable

@Serializable
data object ExclusionsListRoute : NavigationDestination

@Serializable
data class PathExclusionEditorRoute(
    val exclusionId: String? = null,
    val initial: PathExclusionEditorOptions? = null,
) : NavigationDestination

@Serializable
data class PkgExclusionEditorRoute(
    val exclusionId: String? = null,
    val initial: PkgExclusionEditorOptions? = null,
) : NavigationDestination

@Serializable
data class SegmentExclusionEditorRoute(
    val exclusionId: String? = null,
    val initial: SegmentExclusionEditorOptions? = null,
) : NavigationDestination
