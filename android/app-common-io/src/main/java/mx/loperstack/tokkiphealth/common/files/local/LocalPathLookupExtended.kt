package mx.loperstack.tokkiphealth.common.files.local

import android.os.Parcelable
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.APathLookupExtended
import mx.loperstack.tokkiphealth.common.files.Ownership
import mx.loperstack.tokkiphealth.common.files.Permissions
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocalPathLookupExtended(
    val lookup: LocalPathLookup,
    override val ownership: Ownership?,
    override val permissions: Permissions?,
) : APathLookupExtended<LocalPath>, APathLookup<LocalPath> by lookup, Parcelable