package mx.loperstack.tokkiphealth.common.pkgs

import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError

class InvalidPkgInventoryException(
    override val message: String
) : IllegalStateException(), HasLocalizedError {

    override fun getLocalizedError(): LocalizedError = LocalizedError(
        throwable = this,
        label = R.string.pkgrepo_invalid_inventory_error_title.toCaString(),
        description = R.string.pkgrepo_invalid_inventory_error_description.toCaString(),
    )
}