package mx.loperstack.tokkiphealth.stats.core.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.stats.core.AffectedPkg
import mx.loperstack.tokkiphealth.stats.core.ReportId

@Entity(tableName = "affected_pkgs")
data class AffectedPkgEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "report_id") override val reportId: ReportId,
    @ColumnInfo(name = "action") override val action: AffectedPkg.Action,
    @ColumnInfo(name = "pkg_id") override val pkgId: Pkg.Id,
) : AffectedPkg {
    companion object {
        fun from(report: AffectedPkg) = AffectedPkgEntity(
            reportId = report.reportId,
            action = report.action,
            pkgId = report.pkgId,
        )
    }
}