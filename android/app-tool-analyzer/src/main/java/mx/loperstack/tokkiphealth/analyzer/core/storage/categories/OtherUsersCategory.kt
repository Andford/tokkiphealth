package mx.loperstack.tokkiphealth.analyzer.core.storage.categories

import mx.loperstack.tokkiphealth.analyzer.core.content.ContentGroup
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.storage.StorageId
import mx.loperstack.tokkiphealth.common.user.UserHandle2

/**
 * Storage occupied by users/profiles other than the current one.
 *
 * Without this category their bytes are silently absorbed by the system residual, which makes
 * "System data" look inexplicably large on devices with a second user or a work profile.
 */
data class OtherUsersCategory(
    override val storageId: StorageId,
    override val groups: Collection<ContentGroup>,
    val users: Collection<UserEntry>,
    val spaceUsedOverride: Long? = null,
) : ContentCategory {

    override val spaceUsed: Long
        get() = spaceUsedOverride ?: groups.sumOf { it.groupSize }

    /**
     * One entry per other user. Completeness is tracked per user, not per category: a single
     * category-wide "exact" flag can't express "user A was measured, user B is locked", and a
     * stats-only number is exact APP data, not exact user storage.
     */
    data class UserEntry(
        val handle: UserHandle2,
        val label: CaString,
        val groupId: ContentGroup.Id,
        val appDataKnown: Boolean,
        val sharedMediaKnown: Boolean,
        val isBrowsable: Boolean,
    )
}
