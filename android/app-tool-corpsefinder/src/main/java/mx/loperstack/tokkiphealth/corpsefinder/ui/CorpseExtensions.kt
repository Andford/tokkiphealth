package mx.loperstack.tokkiphealth.corpsefinder.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.FolderShared
import androidx.compose.material.icons.twotone.Inventory2
import androidx.compose.material.icons.twotone.LocalLibrary
import androidx.compose.material.icons.twotone.PermMedia
import androidx.compose.material.icons.twotone.SdStorage
import androidx.compose.material.icons.twotone.SportsEsports
import androidx.compose.material.icons.twotone.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector
import mx.loperstack.tokkiphealth.common.compose.icons.Artboard
import mx.loperstack.tokkiphealth.common.compose.icons.Ghost
import mx.loperstack.tokkiphealth.common.compose.icons.SdmIcons
import mx.loperstack.tokkiphealth.common.compose.icons.Turbine
import mx.loperstack.tokkiphealth.corpsefinder.core.filter.AppAsecFileCorpseFilter
import mx.loperstack.tokkiphealth.corpsefinder.core.filter.AppLibCorpseFilter
import mx.loperstack.tokkiphealth.corpsefinder.core.filter.AppSourceCorpseFilter
import mx.loperstack.tokkiphealth.corpsefinder.core.filter.AppSourcePrivateCorpseFilter
import mx.loperstack.tokkiphealth.corpsefinder.core.filter.ArtProfilesCorpseFilter
import mx.loperstack.tokkiphealth.corpsefinder.core.filter.CorpseFilter
import mx.loperstack.tokkiphealth.corpsefinder.core.filter.DalvikCorpseFilter
import mx.loperstack.tokkiphealth.corpsefinder.core.filter.PrivateDataCorpseFilter
import mx.loperstack.tokkiphealth.corpsefinder.core.filter.PublicDataCorpseFilter
import mx.loperstack.tokkiphealth.corpsefinder.core.filter.PublicMediaCorpseFilter
import mx.loperstack.tokkiphealth.corpsefinder.core.filter.PublicObbCorpseFilter
import mx.loperstack.tokkiphealth.corpsefinder.core.filter.SdcardCorpseFilter
import kotlin.reflect.KClass

@get:StringRes
val <T : CorpseFilter> KClass<T>.labelRes: Int
    get() = when (this) {
        SdcardCorpseFilter::class -> mx.loperstack.tokkiphealth.corpsefinder.R.string.corpsefinder_filter_sdcard_label
        PublicMediaCorpseFilter::class -> mx.loperstack.tokkiphealth.corpsefinder.R.string.corpsefinder_filter_publicmedia_label
        PublicDataCorpseFilter::class -> mx.loperstack.tokkiphealth.corpsefinder.R.string.corpsefinder_filter_publicdata_label
        PublicObbCorpseFilter::class -> mx.loperstack.tokkiphealth.corpsefinder.R.string.corpsefinder_filter_publicobb_label
        PrivateDataCorpseFilter::class -> mx.loperstack.tokkiphealth.corpsefinder.R.string.corpsefinder_filter_privatedata_label
        DalvikCorpseFilter::class -> mx.loperstack.tokkiphealth.corpsefinder.R.string.corpsefinder_filter_dalvik_label
        ArtProfilesCorpseFilter::class -> mx.loperstack.tokkiphealth.corpsefinder.R.string.corpsefinder_filter_artprofiles_label
        AppLibCorpseFilter::class -> mx.loperstack.tokkiphealth.corpsefinder.R.string.corpsefinder_filter_applib_label
        AppSourceCorpseFilter::class -> mx.loperstack.tokkiphealth.corpsefinder.R.string.corpsefinder_filter_appsource_label
        AppSourcePrivateCorpseFilter::class -> mx.loperstack.tokkiphealth.corpsefinder.R.string.corpsefinder_filter_appsource_private_label
        AppAsecFileCorpseFilter::class -> mx.loperstack.tokkiphealth.corpsefinder.R.string.corpsefinder_filter_appasec_label
        else -> mx.loperstack.tokkiphealth.common.R.string.general_todo_msg
    }

val <T : CorpseFilter> KClass<T>.icon: ImageVector
    get() = when (this) {
        SdcardCorpseFilter::class -> Icons.TwoTone.SdStorage
        PublicMediaCorpseFilter::class -> Icons.TwoTone.PermMedia
        PublicDataCorpseFilter::class -> Icons.TwoTone.SdStorage
        PublicObbCorpseFilter::class -> Icons.TwoTone.SportsEsports
        PrivateDataCorpseFilter::class -> Icons.TwoTone.VisibilityOff
        DalvikCorpseFilter::class -> SdmIcons.Turbine
        ArtProfilesCorpseFilter::class -> SdmIcons.Artboard
        AppLibCorpseFilter::class -> Icons.TwoTone.LocalLibrary
        AppSourceCorpseFilter::class -> Icons.TwoTone.Inventory2
        AppSourcePrivateCorpseFilter::class -> Icons.TwoTone.FolderShared
        AppAsecFileCorpseFilter::class -> Icons.TwoTone.FolderShared
        else -> SdmIcons.Ghost
    }
