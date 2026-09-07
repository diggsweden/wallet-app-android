// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import eu.europa.ec.eudi.sdjwt.JwtAndClaims
import eu.europa.ec.eudi.sdjwt.SdJwt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import se.digg.wallet.core.services.PresentationResult
import se.digg.wallet.data.PresentationItem

private fun item(id: String, isRequired: Boolean, isChecked: Boolean = false): PresentationItem =
    PresentationItem(
        id = id,
        isChecked = isChecked,
        isRequired = isRequired,
        claims = emptyList(),
        disclosedSdJwt = SdJwt<JwtAndClaims>(
            jwt = "" to JsonObject(emptyMap()),
            disclosures = emptyList(),
        ),
    )

private class FakePresentationService : PresentationService {
    var resolveError: Exception? = null
    var resolveCount = 0
    var result: PresentationResult = PresentationResult.Success
    var presentedItems: List<PresentationItem>? = null
    var presentedPin: String? = null

    val request = PresentationRequest(
        responseUri = "https://verifier.example/response",
        clientId = "verifier.example",
        nonce = "nonce",
        state = "state",
        encryption = null,
        requiredItems = listOf(item("pid", isRequired = true)),
        optionalItems = listOf(
            item("age", isRequired = false),
            item("address", isRequired = false),
        ),
    )

    override suspend fun resolve(uri: String): PresentationRequest {
        resolveCount += 1
        resolveError?.let { error ->
            throw error
        }
        return request
    }

    override suspend fun present(
        request: PresentationRequest,
        items: List<PresentationItem>,
        pin: String,
    ): PresentationResult {
        presentedItems = items
        presentedPin = pin
        return result
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PresentationViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val service = FakePresentationService()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.collectEffects(
        viewModel: PresentationViewModel,
    ): List<PresentationUiEffect> {
        val effects = mutableListOf<PresentationUiEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        return effects
    }

    @Test
    fun `init resolves request and presents claims`() = runTest(dispatcher) {
        val viewModel = PresentationViewModel(service)

        viewModel.init("openid4vp://request")
        advanceUntilIdle()

        assertEquals(
            PresentationUiState.PresentClaims(
                requiredClaims = service.request.requiredItems,
                optionalClaims = service.request.optionalItems,
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `init resolves only once`() = runTest(dispatcher) {
        val viewModel = PresentationViewModel(service)

        viewModel.init("openid4vp://request")
        advanceUntilIdle()
        viewModel.init("openid4vp://request")
        advanceUntilIdle()

        assertEquals(1, service.resolveCount)
    }

    @Test
    fun `resolve failure sets error state`() = runTest(dispatcher) {
        service.resolveError = IllegalStateException("No credential")
        val viewModel = PresentationViewModel(service)

        viewModel.init("openid4vp://request")
        advanceUntilIdle()

        assertEquals(PresentationUiState.Error("No credential"), viewModel.uiState.value)
    }

    @Test
    fun `toggling an optional claim updates only that claim`() = runTest(dispatcher) {
        val viewModel = PresentationViewModel(service)
        viewModel.init("openid4vp://request")
        advanceUntilIdle()

        viewModel.onOptionalClaimCheckedChanged("age", isSelected = true)

        val state = viewModel.uiState.value as PresentationUiState.PresentClaims
        assertEquals(
            listOf(true, false),
            state.optionalClaims.map { it.isChecked },
        )
    }

    @Test
    fun `sendData presents required and checked optional items with the pin`() =
        runTest(dispatcher) {
            val viewModel = PresentationViewModel(service)
            viewModel.init("openid4vp://request")
            advanceUntilIdle()

            viewModel.onOptionalClaimCheckedChanged("address", isSelected = true)
            viewModel.onAccept()
            assertEquals(PresentationUiState.EnterPin, viewModel.uiState.value)

            viewModel.sendData("123456")
            advanceUntilIdle()

            assertEquals(listOf("pid", "address"), service.presentedItems?.map { it.id })
            assertEquals("123456", service.presentedPin)
            assertEquals(PresentationUiState.ShareSuccess, viewModel.uiState.value)
        }

    @Test
    fun `redirect result emits OpenUrl effect`() = runTest(dispatcher) {
        service.result = PresentationResult.Redirect("https://verifier.example/done")
        val viewModel = PresentationViewModel(service)
        val effects = collectEffects(viewModel)
        viewModel.init("openid4vp://request")
        advanceUntilIdle()

        viewModel.onAccept()
        viewModel.sendData("123456")
        advanceUntilIdle()

        assertEquals(
            listOf(PresentationUiEffect.OpenUrl("https://verifier.example/done")),
            effects,
        )
    }

    @Test
    fun `sendData before resolve sets error state`() = runTest(dispatcher) {
        val viewModel = PresentationViewModel(service)

        viewModel.sendData("123456")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is PresentationUiState.Error)
    }
}
