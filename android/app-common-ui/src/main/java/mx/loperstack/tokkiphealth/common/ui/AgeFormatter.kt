package mx.loperstack.tokkiphealth.common.ui

import android.content.Context
import mx.loperstack.tokkiphealth.common.R
import java.time.Duration

fun formatAge(
    context: Context,
    age: Duration,
): String = when {
    age.toDays() > 0 -> {
        context.resources.getQuantityString(
            R.plurals.general_age_days,
            age.toDays().toInt(),
            age.toDays()
        )
    }

    else -> {
        context.resources.getQuantityString(
            R.plurals.general_age_hours,
            age.toHours().toInt(),
            age.toHours()
        )
    }
}
