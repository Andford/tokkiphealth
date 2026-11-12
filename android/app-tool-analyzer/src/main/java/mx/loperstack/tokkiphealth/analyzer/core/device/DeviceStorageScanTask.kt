package mx.loperstack.tokkiphealth.analyzer.core.device

import mx.loperstack.tokkiphealth.analyzer.core.AnalyzerTask
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import kotlinx.parcelize.Parcelize

@Parcelize
data class DeviceStorageScanTask(
    val flag: Boolean = true,
) : AnalyzerTask {

    @Parcelize
    data class Result(
        private val itemCount: Int,
    ) : AnalyzerTask.Result {
        override val primaryInfo: CaString
            get() = mx.loperstack.tokkiphealth.common.R.string.general_result_success_message.toCaString()
    }
}