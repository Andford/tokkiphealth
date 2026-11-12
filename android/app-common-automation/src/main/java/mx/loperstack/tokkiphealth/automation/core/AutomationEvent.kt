package mx.loperstack.tokkiphealth.automation.core

import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import java.util.UUID

/**
 * Abstraction for automation events, allowing testability without Android framework dependencies.
 *
 * In production, [AutomationService.Snapshot] implements this with real [android.view.accessibility.AccessibilityEvent].
 * In tests, [TestAutomationEvent] provides a simple implementation.
 */
interface AutomationEvent {
    val id: UUID
    val pkgId: Pkg.Id?
    val eventType: Int
}
