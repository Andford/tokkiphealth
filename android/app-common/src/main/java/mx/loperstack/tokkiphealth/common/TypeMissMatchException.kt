package mx.loperstack.tokkiphealth.common

import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError

class TypeMissMatchException(private val expected: Any, private val actual: Any) :
    IllegalArgumentException("Type missmatch: Wanted $expected, but got $actual."), HasLocalizedError {

    override fun getLocalizedError() = LocalizedError(
        throwable = this,
        label = "TypeMissMatchException".toCaString(),
        description = caString { it.getString(R.string.general_error_type_mismatch_msg, expected, actual) }
    )

    companion object {
        fun check(expected: Any, actual: Any) {
            if (expected != actual) throw TypeMissMatchException(expected, actual)
        }
    }
}