package mx.loperstack.tokkiphealth.automation.core.errors

import android.os.Build
import mx.loperstack.tokkiphealth.automation.R
import mx.loperstack.tokkiphealth.common.BuildConfigWrap
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError
import mx.loperstack.tokkiphealth.setup.SetupModule
import mx.loperstack.tokkiphealth.setup.SetupRoute
import mx.loperstack.tokkiphealth.setup.SetupScreenOptions

open class AutomationCompatibilityException(
    override val message: String = "Tokkip Health couldn't figure out the screen layout. If this keeps happening, your language or setup might not be fully supported yet.",
    private val additionalHint: CaString? = null,
    val suggestAdbSetup: Boolean = false,
) : AutomationException(), HasLocalizedError {

    override fun getLocalizedError(): LocalizedError = LocalizedError(
        throwable = this,
        label = R.string.automation_error_compatibility_title.toCaString(),
        description = caString {
            val sb = StringBuilder()
            sb.append(getString(R.string.automation_error_compatibility_body))
            additionalHint?.let { hint ->
                sb.append("\n\n")
                sb.append(hint.get(this))
            }
            if (suggestAdbSetup) {
                sb.append("\n\n")
                sb.append(getString(R.string.automation_error_compatibility_adb_hint))
            }
            sb.append("\n\n\n")
            sb.append(
                """
                    ${getString(mx.loperstack.tokkiphealth.common.R.string.general_information_for_the_developer)}:
                    v${BuildConfigWrap.VERSION_NAME} (${BuildConfigWrap.VERSION_CODE}) ${BuildConfigWrap.FLAVOR} [${BuildConfigWrap.BUILD_TYPE}]
                    ${Build.FINGERPRINT}
                """.trimIndent()
            )
            sb.toString()
        },
        fixActionLabel = mx.loperstack.tokkiphealth.common.R.string.setup_title.toCaString().takeIf { suggestAdbSetup },
        fixActionRoute = SetupRoute(
            options = SetupScreenOptions(
                showCompleted = true,
                typeFilter = setOf(SetupModule.Type.SHIZUKU),
            ),
        ).takeIf { suggestAdbSetup },
        infoActionLabel = mx.loperstack.tokkiphealth.common.R.string.general_error_report_bug_action.toCaString(),
        infoAction = { WebpageTool.open(it, "https://sdmse.darken.eu/bugs") }
    )

}
