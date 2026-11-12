package mx.loperstack.tokkiphealth.common.review

import android.app.Activity
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FossReviewTool @Inject constructor() : ReviewTool {


    override val state: Flow<ReviewTool.State> = flowOf(ReviewTool.State())

    override suspend fun dismiss() {
        log(TAG, INFO) { "dismiss()" }
        // NOOP
    }

    override suspend fun reviewNow(activity: Activity) {
        log(TAG, INFO) { "reviewNow($activity)" }
        // NOOP
    }

    companion object {
        private val TAG = logTag("Review", "Tool", "FOSS")
    }
}