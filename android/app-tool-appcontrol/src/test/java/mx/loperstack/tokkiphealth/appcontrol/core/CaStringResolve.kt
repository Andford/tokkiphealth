package mx.loperstack.tokkiphealth.appcontrol.core

import android.content.Context
import android.content.res.Resources
import mx.loperstack.tokkiphealth.common.ca.CaString
import io.mockk.every
import io.mockk.mockk

/**
 * Resolves a [CaString] without Robolectric by faking the plural lookup that
 * `Context.getQuantityString2` performs, tagging each clause with its resource.
 */
internal fun CaString.resolve(): String {
    val res = mockk<Resources>().apply {
        every { getQuantityString(any(), any(), *anyVararg()) } answers {
            val quantity = secondArg<Int>()
            val name = when (firstArg<Int>()) {
                mx.loperstack.tokkiphealth.appcontrol.R.plurals.appcontrol_toggle_result_message_x -> "toggled"
                mx.loperstack.tokkiphealth.appcontrol.R.plurals.appcontrol_toggle_result_skipped_x -> "skipped"
                mx.loperstack.tokkiphealth.appcontrol.R.plurals.appcontrol_force_stop_result_message_x -> "stopped"
                mx.loperstack.tokkiphealth.appcontrol.R.plurals.appcontrol_archive_result_x -> "archived"
                mx.loperstack.tokkiphealth.appcontrol.R.plurals.appcontrol_restore_result_x -> "restored"
                mx.loperstack.tokkiphealth.appcontrol.R.plurals.appcontrol_uninstall_result_message_x -> "uninstalled"
                mx.loperstack.tokkiphealth.appcontrol.R.plurals.appcontrol_export_result_message_x -> "exported"
                mx.loperstack.tokkiphealth.common.R.plurals.result_x_failed -> "failed"
                else -> "unknown"
            }
            "$quantity $name"
        }
    }
    return get(mockk<Context>().apply { every { resources } returns res })
}
