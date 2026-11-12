package mx.loperstack.tokkiphealth.analyzer.core.storage

import mx.loperstack.tokkiphealth.analyzer.core.AnalyzerTask
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.storage.StorageId
import kotlinx.parcelize.Parcelize

@Parcelize
data class SystemDeepScanTask(
    val storageId: StorageId,
) : AnalyzerTask {

    @Parcelize
    data class Result(
        val success: Boolean,
    ) : AnalyzerTask.Result {
        override val primaryInfo: CaString
            get() = mx.loperstack.tokkiphealth.common.R.string.general_result_success_message.toCaString()
    }
}
