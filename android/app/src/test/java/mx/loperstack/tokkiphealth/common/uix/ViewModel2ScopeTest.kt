package mx.loperstack.tokkiphealth.common.uix

import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.files.ReadException
import mx.loperstack.tokkiphealth.common.flow.replayingShare
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import testhelpers.BaseTest
import testhelpers.coroutine.TestDispatcherProvider
import testhelpers.coroutine.runTest2
import testhelpers.livedata.InstantExecutorExtension

@ExtendWith(InstantExecutorExtension::class)
class ViewModel2ScopeTest : BaseTest() {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `vmScope catches exceptions from replayingShare flows`() = runTest2(
        context = testDispatcher,
    ) {
        val vm = TestViewModel(TestDispatcherProvider(testDispatcher))

        vm.state.first()
        advanceUntilIdle()

        val postedError = vm.errorEvents.first()
        postedError.shouldBeInstanceOf<ReadException>()
    }

    private class TestViewModel(
        dispatcherProvider: DispatcherProvider,
    ) : ViewModel4(dispatcherProvider) {
        val state = flow<String> {
            throw ReadException(message = "No matching mode available.")
        }
            .onStart { emit("loading") }
            .replayingShare(vmScope)
    }
}
