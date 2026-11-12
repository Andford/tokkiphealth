package mx.loperstack.tokkiphealth.appcleaner.core

import mx.loperstack.tokkiphealth.appcleaner.core.forensics.ExpendablesFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.ExpendablesFilterIdentifier
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.DefaultCachesPrivateFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.DefaultCachesPublicFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.ThumbnailsFilter
import mx.loperstack.tokkiphealth.appcleaner.ui.preview.previewAppJunk
import mx.loperstack.tokkiphealth.appcleaner.ui.preview.previewInaccessibleCache
import mx.loperstack.tokkiphealth.common.files.FileType
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.files.local.LocalPathLookup
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import testhelpers.BaseTest
import java.time.Instant

class AppJunkTest : BaseTest() {

    private fun match(
        identifier: ExpendablesFilterIdentifier,
        path: String,
    ): ExpendablesFilter.Match = ExpendablesFilter.Match.Deletion(
        identifier = identifier,
        lookup = LocalPathLookup(
            lookedUp = LocalPath.build(path),
            fileType = FileType.FILE,
            size = 1024L,
            modifiedAt = Instant.EPOCH,
            target = null,
        ),
    )

    @Test
    fun `limitToTrimBlastRadius keeps only the default cache filters`() {
        val publicMatch = match(DefaultCachesPublicFilter::class, "/public")
        val privateMatch = match(DefaultCachesPrivateFilter::class, "/private")
        val thumbnailMatch = match(ThumbnailsFilter::class, "/thumbnail")

        val narrowed = previewAppJunk(
            expendables = mapOf(
                DefaultCachesPublicFilter::class to listOf(publicMatch),
                DefaultCachesPrivateFilter::class to listOf(privateMatch),
                ThumbnailsFilter::class to listOf(thumbnailMatch),
            ),
        ).limitToTrimBlastRadius()!!

        narrowed.expendables!!.keys.toList() shouldContainExactlyInAnyOrder listOf(
            DefaultCachesPublicFilter::class,
            DefaultCachesPrivateFilter::class,
        )
        narrowed.expendables!!.values.flatten() shouldContainExactlyInAnyOrder listOf(publicMatch, privateMatch)
    }

    @Test
    fun `limitToTrimBlastRadius flags the junk and keeps the inaccessible cache`() {
        val cache = previewInaccessibleCache()
        val junk = previewAppJunk(
            expendables = mapOf(DefaultCachesPublicFilter::class to listOf(match(DefaultCachesPublicFilter::class, "/public"))),
            inaccessibleCache = cache,
        )

        val narrowed = junk.limitToTrimBlastRadius()!!

        narrowed.isExclusionLimited shouldBe true
        narrowed.inaccessibleCache shouldBe cache
        junk.isExclusionLimited shouldBe false
    }

    @Test
    fun `limitToTrimBlastRadius keeps an inaccessible-only junk`() {
        val narrowed = previewAppJunk(
            expendables = mapOf(ThumbnailsFilter::class to listOf(match(ThumbnailsFilter::class, "/thumbnail"))),
            inaccessibleCache = previewInaccessibleCache(),
        ).limitToTrimBlastRadius()!!

        narrowed.expendables shouldBe null
        narrowed.isExclusionLimited shouldBe true
    }

    @Test
    fun `limitToTrimBlastRadius returns null without at-risk content`() {
        previewAppJunk(
            expendables = mapOf(ThumbnailsFilter::class to listOf(match(ThumbnailsFilter::class, "/thumbnail"))),
            inaccessibleCache = null,
        ).limitToTrimBlastRadius() shouldBe null

        previewAppJunk(
            expendables = null,
            inaccessibleCache = null,
        ).limitToTrimBlastRadius() shouldBe null
    }
}
