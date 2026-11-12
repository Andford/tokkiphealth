package mx.loperstack.tokkiphealth.automation.core.errors

import mx.loperstack.tokkiphealth.setup.SetupModule
import mx.loperstack.tokkiphealth.setup.SetupRoute
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import testhelpers.BaseTest

class AutomationCompatibilityExceptionTest : BaseTest() {

    @Test
    fun `without the ADB suggestion there is no setup action`() {
        val error = AutomationCompatibilityException().getLocalizedError()

        error.fixActionLabel.shouldBeNull()
        error.fixActionRoute.shouldBeNull()
        error.infoAction.shouldNotBeNull()
    }

    @Test
    fun `the ADB suggestion opens setup on the ADB access card`() {
        val error = AutomationCompatibilityException(suggestAdbSetup = true).getLocalizedError()

        error.fixActionLabel.shouldNotBeNull()
        val route = error.fixActionRoute.shouldBeInstanceOf<SetupRoute>()
        route.options!!.typeFilter shouldBe setOf(SetupModule.Type.SHIZUKU)
        error.infoAction.shouldNotBeNull()
    }
}
