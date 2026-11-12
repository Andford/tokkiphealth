package mx.loperstack.tokkiphealth.swiper.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import mx.loperstack.tokkiphealth.common.room.APathListTypeConverter
import mx.loperstack.tokkiphealth.common.room.APathTypeConverter
import mx.loperstack.tokkiphealth.common.room.InstantConverter

@Database(
    entities = [
        SwipeSessionEntity::class,
        SwipeItemEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(
    InstantConverter::class,
    SwipeDecisionConverter::class,
    SessionStateConverter::class,
    APathTypeConverter::class,
    APathListTypeConverter::class,
    FileTypeFilterConverter::class,
    SortOrderConverter::class,
)
abstract class SwiperRoomDb : RoomDatabase() {
    abstract fun sessions(): SwipeSessionDao
    abstract fun items(): SwipeItemDao
}
