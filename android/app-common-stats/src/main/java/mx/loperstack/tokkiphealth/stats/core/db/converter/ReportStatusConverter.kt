package mx.loperstack.tokkiphealth.stats.core.db.converter

import androidx.room.TypeConverter
import mx.loperstack.tokkiphealth.stats.core.Report

class ReportStatusConverter {
    @TypeConverter
    fun from(value: Report.Status): String = value.name

    @TypeConverter
    fun to(value: String): Report.Status = Report.Status.valueOf(value)
}