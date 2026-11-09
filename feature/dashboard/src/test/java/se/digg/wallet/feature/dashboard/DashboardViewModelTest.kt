// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.dashboard

import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import se.digg.wallet.core.data.CredentialStore
import se.digg.wallet.core.model.CredentialDisplayData
import se.digg.wallet.core.model.SavedCredential

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val credentialsFlow = MutableStateFlow<List<SavedCredential>>(emptyList())
    private val fakeCredentialStore = object : CredentialStore {
        override val credentials: Flow<List<SavedCredential>> = credentialsFlow
        override suspend fun getCredentials(): List<SavedCredential> = credentialsFlow.value
        override suspend fun addCredentials(credentials: List<SavedCredential>) {
            credentialsFlow.value += credentials
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createCredential(id: String, name: String): SavedCredential = SavedCredential(
        id = id,
        compactSerialized = "raw-$id",
        claimDisplayNames = emptyMap(),
        issuer = null,
        displayData = CredentialDisplayData(name),
        keyId = "key-$id",
    )

    @Test
    fun initialUiState_whenEmpty_emitsNullPidAndEmptyCredentials() = runTest {
        val viewModel = DashboardViewModel(fakeCredentialStore)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.pid)
        assertEquals(emptyList<SavedCredential>(), viewModel.uiState.value.credentials)
    }

    @Test
    fun uiState_withSingleCredential_emitsPidAndEmptyCredentials() = runTest {
        val cred1 = createCredential("pid-1", "Personal Identity")
        credentialsFlow.value = listOf(cred1)

        val viewModel = DashboardViewModel(fakeCredentialStore)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(cred1, viewModel.uiState.value.pid)
        assertEquals(emptyList<SavedCredential>(), viewModel.uiState.value.credentials)
    }

    @Test
    fun uiState_withMultipleCredentials_emitsFirstAsPidAndRestAsCredentials() = runTest {
        val pid = createCredential("pid-1", "PID")
        val cred1 = createCredential("doc-1", "Driver License")
        val cred2 = createCredential("doc-2", "Diploma")
        credentialsFlow.value = listOf(pid, cred1, cred2)

        val viewModel = DashboardViewModel(fakeCredentialStore)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(pid, viewModel.uiState.value.pid)
        assertEquals(listOf(cred1, cred2), viewModel.uiState.value.credentials)
    }

    @Test
    fun uiState_updatesDynamicallyWhenCredentialsChange() = runTest {
        val viewModel = DashboardViewModel(fakeCredentialStore)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.pid)

        val pid = createCredential("pid-1", "PID")
        val cred = createCredential("doc-1", "Passport")
        credentialsFlow.value = listOf(pid, cred)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(pid, viewModel.uiState.value.pid)
        assertEquals(listOf(cred), viewModel.uiState.value.credentials)
    }

    @Test
    fun formatDate_formatsDateCorrectly() {
        val localDate = LocalDate.of(2026, 11, 6)
        val date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val formatted = formatDate(date)

        assertTrue(formatted.startsWith("06 "))
        assertTrue(formatted.endsWith(" 2026"))
    }
}
