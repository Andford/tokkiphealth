package mx.loperstack.tokkiphealth.common.clutter.manual

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.clutter.MarkerSource
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import javax.inject.Inject

@Reusable
class ProductionMarkerSource @Inject constructor(
    pkgRepo: PkgRepo,
    jsonMarkerParser: JsonMarkerParser,
) : ManualMarkerSource(
    pkgRepo,
    { jsonMarkerParser.fromAssets("clutter/db_clutter_markers.json") }
) {
    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun source(source: ProductionMarkerSource): MarkerSource
    }
}