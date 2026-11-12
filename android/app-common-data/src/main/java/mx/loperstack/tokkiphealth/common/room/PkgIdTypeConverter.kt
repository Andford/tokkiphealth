package mx.loperstack.tokkiphealth.common.room

import androidx.room.TypeConverter
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId

class PkgIdTypeConverter {
    @TypeConverter
    fun from(value: Pkg.Id): String = value.name

    @TypeConverter
    fun to(value: String): Pkg.Id = value.toPkgId()
}