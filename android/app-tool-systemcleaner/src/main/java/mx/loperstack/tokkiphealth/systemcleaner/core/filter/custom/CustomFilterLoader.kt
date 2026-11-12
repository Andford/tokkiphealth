package mx.loperstack.tokkiphealth.systemcleaner.core.filter.custom

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleanerSettings
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilter

class CustomFilterLoader @AssistedInject constructor(
    @Assisted private val filterConfig: CustomFilterConfig,
    private val factory: CustomFilter.Factory,
    private val settings: SystemCleanerSettings,
) : SystemCleanerFilter.Factory {

    override suspend fun isEnabled(): Boolean = settings.isCustomFilterEnabled(filterConfig.identifier)

    override suspend fun create(): SystemCleanerFilter = factory.create(filterConfig)

    @AssistedFactory
    interface Factory {
        fun create(filterConfig: CustomFilterConfig): CustomFilterLoader
    }
}