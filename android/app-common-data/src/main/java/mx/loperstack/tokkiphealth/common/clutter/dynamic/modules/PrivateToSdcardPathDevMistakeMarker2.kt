package mx.loperstack.tokkiphealth.common.clutter.dynamic.modules

import android.annotation.SuppressLint
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

/**
 * /sdcard/data/user/0/com.some.pkg
 */
@SuppressLint("SdCardPath")
@Reusable
class PrivateToSdcardPathDevMistakeMarker2 @Inject constructor() : NestedPackageMatcher(
    DataArea.Type.SDCARD,
    listOf("data", "user", "0"),
    emptySet()
) {
    override fun toString(): String = "PrivateToSdcardPathDevMistakeMarker2"

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun source(source: PrivateToSdcardPathDevMistakeMarker2): MarkerSource
    }
}