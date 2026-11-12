package mx.loperstack.tokkiphealth.squeezer.core.backup

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.backup.DatabaseBackupContributor
import mx.loperstack.tokkiphealth.common.room.backup.RoomDbBackupContributor
import mx.loperstack.tokkiphealth.squeezer.core.history.CompressionHistoryDatabase
import javax.inject.Inject
import javax.inject.Singleton

/** Backs up the compression history (hashes of already-compressed media) to avoid recompression after a restore. */
@Singleton
class CompressionHistoryDbBackupContributor @Inject constructor(
    @ApplicationContext context: Context,
    db: CompressionHistoryDatabase,
) : RoomDbBackupContributor(
    sqliteProvider = { db.roomDb.openHelper.writableDatabase },
    dbFileProvider = { context.getDatabasePath("compression_history") },
    tables = listOf("compression_history"),
    onRestored = { db.roomDb.invalidationTracker.refreshAsync() },
) {
    override val key = "squeezer.db"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CompressionHistoryDbBackupModule {
    @Binds
    @IntoSet
    abstract fun bind(c: CompressionHistoryDbBackupContributor): DatabaseBackupContributor
}
