package mx.loperstack.tokkiphealth.main.core

import mx.loperstack.tokkiphealth.analyzer.ui.AppDetailsRoute
import mx.loperstack.tokkiphealth.analyzer.ui.ContentRoute
import mx.loperstack.tokkiphealth.analyzer.ui.StorageContentRoute
import mx.loperstack.tokkiphealth.analyzer.core.content.ContentGroup
import mx.loperstack.tokkiphealth.common.navigation.routes.DashboardRoute
import mx.loperstack.tokkiphealth.common.navigation.routes.UpgradeRoute
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import mx.loperstack.tokkiphealth.common.storage.StorageId
import mx.loperstack.tokkiphealth.common.user.UserHandle2
import mx.loperstack.tokkiphealth.corpsefinder.ui.CorpseDetailsRoute
import mx.loperstack.tokkiphealth.deduplicator.ui.DeduplicatorDetailsRoute
import mx.loperstack.tokkiphealth.deduplicator.core.Duplicate
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.scheduler.ui.ScheduleItemRoute
import mx.loperstack.tokkiphealth.setup.SetupRoute
import mx.loperstack.tokkiphealth.setup.SetupScreenOptions
import mx.loperstack.tokkiphealth.setup.SetupModule
import mx.loperstack.tokkiphealth.swiper.ui.SwiperSwipeRoute
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import testhelpers.BaseTest
import java.util.UUID

class NavigationIdConsistencyTest : BaseTest() {

    @Test
    fun `object routes are singletons`() {
        DashboardRoute shouldNotBe null
    }

    @Test
    fun `data class routes with defaults are constructable`() {
        UpgradeRoute() shouldBe UpgradeRoute(forced = false)
        SetupRoute() shouldBe SetupRoute(options = null)
        CorpseDetailsRoute() shouldBe CorpseDetailsRoute(corpsePathJson = null)
    }

    @Test
    fun `key routes serialize and deserialize correctly`() {
        val storageId = StorageId(internalId = "primary", externalId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
        val installId = InstallId(pkgId = Pkg.Id("com.example"), userHandle = UserHandle2(0))

        val routes = listOf(
            UpgradeRoute(forced = true),
            SetupRoute(options = SetupScreenOptions(isOnboarding = true, typeFilter = setOf(SetupModule.Type.ROOT))),
            StorageContentRoute(storageId = storageId),
            AppDetailsRoute(storageId = storageId, installId = installId),
            ContentRoute(storageId = storageId, groupId = ContentGroup.Id("group-1"), installId = installId),
            DeduplicatorDetailsRoute(identifier = Duplicate.Cluster.Id("cluster-1")),
            ScheduleItemRoute(scheduleId = "sched-1"),
            SwiperSwipeRoute(sessionId = "session-1", startIndex = 5),
            CorpseDetailsRoute(corpsePath = LocalPath.build("/data/test")),
        )

        routes.forEach { route ->
            val json = Json.encodeToString(kotlinx.serialization.serializer(route::class.java), route)
            json.isNotBlank() shouldBe true
        }
    }
}
