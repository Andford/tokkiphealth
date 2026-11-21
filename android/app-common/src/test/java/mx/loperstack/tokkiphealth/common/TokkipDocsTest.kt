package mx.loperstack.tokkiphealth.common

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TokkipDocsTest {
    private val base = TokkipDocs.BASE

    @Test
    fun `pages and anchors keep their names`() {
        TokkipDocs.redirect("https://sdmse.darken.eu/setup#shizuku") shouldBe "${base}setup.md#shizuku"
        TokkipDocs.redirect("https://sdmse.darken.eu/appcleaner#settings") shouldBe "${base}appcleaner.md#settings"
        TokkipDocs.redirect("https://sdmse.darken.eu/de/setup") shouldBe "${base}setup.md"
    }

    @Test
    fun `the rest goes to the index, releases or issues`() {
        TokkipDocs.redirect("https://sdmse.darken.eu/") shouldBe "${base}README.md"
        TokkipDocs.redirect("https://sdmse.darken.eu/changelog") shouldBe "https://github.com/Andford/tokkiphealth/releases"
        TokkipDocs.redirect("https://example.com/x") shouldBe "https://example.com/x"
        TokkipDocs.redirect("https://sdmse.darken.eu.example.com/setup") shouldBe "https://sdmse.darken.eu.example.com/setup"
    }
}
