package mx.loperstack.tokkiphealth.common.clutter.dynamic.modules

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.clutter.MarkerSource
import mx.loperstack.tokkiphealth.common.clutter.dynamic.NestedPackageMatcher
import javax.inject.Inject

@Reusable
class UTSystemConfigMarkerMatcher @Inject constructor() : NestedPackageMatcher(
    DataArea.Type.SDCARD,
    listOf(".UTSystemConfig"),
    emptySet(),
) {
    override fun toString(): String = "UTSystemConfigMarkerSource"

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun source(source: UTSystemConfigMarkerMatcher): MarkerSource
    }
}