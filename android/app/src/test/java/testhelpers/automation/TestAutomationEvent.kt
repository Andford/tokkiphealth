package testhelpers.automation

import mx.loperstack.tokkiphealth.automation.core.AutomationEvent
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import java.util.UUID

/**
 * Test implementation of [AutomationEvent] for testing automation specs.
 */
data class TestAutomationEvent(
    override val id: UUID = UUID.randomUUID(),
    override val pkgId: Pkg.Id,
    override val eventType: Int = android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
) : AutomationEvent {

    companion object {
        fun fromPkgId(pkgId: String): TestAutomationEvent = TestAutomationEvent(
            pkgId = pkgId.toPkgId()
        )
    }
}
