package mx.loperstack.tokkiphealth.common.uix

import androidx.lifecycle.viewModelScope
import mx.loperstack.tokkiphealth.common.coroutine.DefaultDispatcherProvider
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.flow.setupCommonEventHandlers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.coroutines.CoroutineContext

abstract class ViewModel2(
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
    override val tag: String = defaultTag(),
) : ViewModel1(tag = tag) {

    var launchErrorHandler: CoroutineExceptionHandler? = null

    private fun getVMContext(): CoroutineContext {
        val dispatcher = dispatcherProvider.Default
        return launchErrorHandler?.let { dispatcher + it } ?: dispatcher
    }

    val vmScope: CoroutineScope by lazy {
        viewModelScope + getVMContext()
    }

    fun launch(
        scope: CoroutineScope = viewModelScope,
        context: CoroutineContext = getVMContext(),
        block: suspend CoroutineScope.() -> Unit,
    ) {
        try {
            scope.launch(context = context, block = block)
        } catch (e: CancellationException) {
            log(tag, WARN) { "launch()ed coroutine was canceled (scope=$scope): ${e.asLog()}" }
        }
    }

    open fun <T> Flow<T>.launchInViewModel() = this
        .setupCommonEventHandlers(tag) { "launchInViewModel()" }
        .launchIn(vmScope)

    companion object {
        private fun defaultTag(): String = logTag("ViewModel")
    }
}
