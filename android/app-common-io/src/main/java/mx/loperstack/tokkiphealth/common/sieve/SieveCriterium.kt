package mx.loperstack.tokkiphealth.common.sieve

import android.os.Parcelable

sealed interface SieveCriterium : Criterium, Parcelable {
    val mode: Mode

    sealed interface Mode : Parcelable
}