package mx.loperstack.tokkiphealth.setup

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class SetupScreenOptions(
    val typeFilter: Set<SetupModule.Type>? = null,
    val isOnboarding: Boolean = false,
    val showCompleted: Boolean = false,
) : Parcelable
