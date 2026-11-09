// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.dashboard

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import se.digg.wallet.core.data.CredentialStore
import se.digg.wallet.core.model.CredentialDisplayData
import se.digg.wallet.core.model.SavedCredential

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()) :
    TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val credentialsFlow = MutableSharedFlow<List<SavedCredential>>(replay = 1)
    private val fakeCredentialStore = object : CredentialStore {
        override val credentials: Flow<List<SavedCredential>> = credentialsFlow
        override suspend fun getCredentials(): List<SavedCredential> = emptyList()
        override suspend fun addCredentials(credentials: List<SavedCredential>) {}
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
    fun initialUiState_isInitiallyLoading() {
        val viewModel = DashboardViewModel(fakeCredentialStore)
        assertEquals(DashboardUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun uiState_whenStoreEmitsEmpty_transitionsFromLoadingToEmptySuccess() = runTest {
        val viewModel = DashboardViewModel(fakeCredentialStore)
        viewModel.uiState.test {
            assertEquals(DashboardUiState.Loading, awaitItem())

            credentialsFlow.emit(emptyList())
            val success = awaitItem() as DashboardUiState.Success
            assertNull(success.pid)
            assertTrue(success.credentials.isEmpty())
        }
    }

    @Test
    fun uiState_withSingleCredential_emitsPidAndEmptyCredentials() = runTest {
        val cred1 = createCredential("pid-1", "Personal Identity")
        val viewModel = DashboardViewModel(fakeCredentialStore)

        viewModel.uiState.test {
            assertEquals(DashboardUiState.Loading, awaitItem())

            credentialsFlow.emit(listOf(cred1))
            val success = awaitItem() as DashboardUiState.Success
            assertEquals(cred1, success.pid)
            assertTrue(success.credentials.isEmpty())
        }
    }

    @Test
    fun uiState_withMultipleCredentials_emitsFirstAsPidAndRestAsCredentials() = runTest {
        val pid = createCredential("pid-1", "PID")
        val cred1 = createCredential("doc-1", "Driver License")
        val cred2 = createCredential("doc-2", "Diploma")
        val viewModel = DashboardViewModel(fakeCredentialStore)

        viewModel.uiState.test {
            assertEquals(DashboardUiState.Loading, awaitItem())

            credentialsFlow.emit(listOf(pid, cred1, cred2))
            val success = awaitItem() as DashboardUiState.Success
            assertEquals(pid, success.pid)
            assertEquals(listOf(cred1, cred2), success.credentials)
        }
    }

    @Test
    fun uiState_updatesDynamicallyWhenCredentialsChange() = runTest {
        val viewModel = DashboardViewModel(fakeCredentialStore)

        viewModel.uiState.test {
            assertEquals(DashboardUiState.Loading, awaitItem())

            credentialsFlow.emit(emptyList())
            val emptySuccess = awaitItem() as DashboardUiState.Success
            assertNull(emptySuccess.pid)

            val pid = createCredential("pid-1", "PID")
            val cred = createCredential("doc-1", "Passport")
            credentialsFlow.emit(listOf(pid, cred))

            val updatedSuccess = awaitItem() as DashboardUiState.Success
            assertEquals(pid, updatedSuccess.pid)
            assertEquals(listOf(cred), updatedSuccess.credentials)
        }
    }

    @Test
    fun uiState_whenUpstreamThrows_emitsError() = runTest {
        val failingStore = object : CredentialStore {
            override val credentials: Flow<List<SavedCredential>> = flow {
                throw IllegalStateException("Database read failure")
            }

            override suspend fun getCredentials(): List<SavedCredential> = emptyList()
            override suspend fun addCredentials(credentials: List<SavedCredential>) {}
        }

        val viewModel = DashboardViewModel(failingStore)
        viewModel.uiState.test {
            val error = awaitItem() as DashboardUiState.Error
            assertEquals("Database read failure", error.message)
        }
    }
}
