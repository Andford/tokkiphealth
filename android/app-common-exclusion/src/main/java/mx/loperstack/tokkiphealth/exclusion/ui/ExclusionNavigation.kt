package mx.loperstack.tokkiphealth.exclusion.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry
import mx.loperstack.tokkiphealth.exclusion.ui.editor.path.PathExclusionEditorScreenHost
import mx.loperstack.tokkiphealth.exclusion.ui.editor.pkg.PkgExclusionEditorScreenHost
import mx.loperstack.tokkiphealth.exclusion.ui.editor.segment.SegmentExclusionEditorScreenHost
import mx.loperstack.tokkiphealth.exclusion.ui.list.ExclusionListScreenHost
import javax.inject.Inject

class ExclusionNavigation @Inject constructor() : NavigationEntry {

    override fun EntryProviderScope<NavKey>.setup() {
        entry<ExclusionsListRoute> { ExclusionListScreenHost() }
        entry<PathExclusionEditorRoute> { route -> PathExclusionEditorScreenHost(route = route) }
        entry<PkgExclusionEditorRoute> { route -> PkgExclusionEditorScreenHost(route = route) }
        entry<SegmentExclusionEditorRoute> { route -> SegmentExclusionEditorScreenHost(route = route) }
    }
}
