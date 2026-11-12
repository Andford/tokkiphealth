package mx.loperstack.tokkiphealth.swiper.core.backup

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.backup.DatabaseBackupContributor
import mx.loperstack.tokkiphealth.common.room.backup.RoomDbBackupContributor
import mx.loperstack.tokkiphealth.swiper.core.db.SwiperRoomDb
import javax.inject.Inject
import javax.inject.Singleton

/** Backs up swipe sessions + their items (valid on a same-device restore; cross-device sessions point at absent paths). */
@Singleton
class SwipeDbBackupContributor @Inject constructor(
    @ApplicationContext context: Context,
    db: SwiperRoomDb,
) : RoomDbBackupContributor(
    sqliteProvider = { db.openHelper.writableDatabase },
    dbFileProvider = { context.getDatabasePath("swiper.db") },
    tables = listOf("swipe_sessions", "swipe_items"),
    onRestored = { db.invalidationTracker.refreshAsync() },
) {
    override val key = "swiper.db"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SwipeDbBackupModule {
    @Binds
    @IntoSet
    abstract fun bind(c: SwipeDbBackupContributor): DatabaseBackupContributor
}
