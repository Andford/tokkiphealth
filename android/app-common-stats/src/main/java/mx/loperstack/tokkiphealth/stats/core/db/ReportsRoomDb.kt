package mx.loperstack.tokkiphealth.stats.core.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import mx.loperstack.tokkiphealth.common.room.APathTypeConverter
import mx.loperstack.tokkiphealth.common.room.InstantConverter
import mx.loperstack.tokkiphealth.common.room.PkgIdTypeConverter
import mx.loperstack.tokkiphealth.common.room.SDMToolTypeConverter
import mx.loperstack.tokkiphealth.stats.core.db.converter.AffectedFileActionConverter
import mx.loperstack.tokkiphealth.stats.core.db.converter.ReportIdTypeConverter
import mx.loperstack.tokkiphealth.stats.core.db.converter.ReportStatusConverter

@Database(
    entities = [
        ReportEntity::class,
        AffectedPathEntity::class,
        AffectedPkgEntity::class,
        SpaceSnapshotEntity::class,
    ],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
    exportSchema = true,
)
@TypeConverters(
    InstantConverter::class,
    ReportIdTypeConverter::class,
    SDMToolTypeConverter::class,
    ReportStatusConverter::class,
    AffectedFileActionConverter::class,
    APathTypeConverter::class,
    PkgIdTypeConverter::class,
)
abstract class ReportsRoomDb : RoomDatabase() {
    abstract fun reports(): ReportsDao
    abstract fun paths(): AffectedPathsDao
    abstract fun pkgs(): AffectedPkgsDao
    abstract fun spaceSnapshots(): SpaceSnapshotDao
}