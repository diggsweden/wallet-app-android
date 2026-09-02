// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.dashboard

import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import se.digg.wallet.core.storage.user.User
import se.digg.wallet.data.CredentialType
import se.digg.wallet.data.SavedCredential
import se.digg.wallet.data.UserRepository
import se.digg.wallet.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val users = MutableStateFlow<User?>(null)
    private val userRepository = mockk<UserRepository>().also {
        every { it.user } returns users
    }

    private fun credential(id: String) = SavedCredential(
        compactSerialized = "sd-jwt",
        claimDisplayNames = emptyMap(),
        issuer = null,
        type = CredentialType.PID.type,
        id = id,
        displayData = null,
    )

    private fun user(pid: SavedCredential?, credentials: List<SavedCredential>) = User(
        uuid = null,
        accountId = "account",
        credentials = credentials,
        pid = pid,
    )

    @Test
    fun `starts empty before the first user emission`() {
        val state = DashboardViewModel(userRepository).uiState.value

        assertNull(state.pid)
        assertEquals(emptyList<SavedCredential>(), state.credentials)
    }

    @Test
    fun `projects the stored user into pid and credentials`() = runTest {
        val vm = DashboardViewModel(userRepository)
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        val pid = credential("pid-1")
        val other = credential("cred-2")
        users.value = user(pid = pid, credentials = listOf(other))

        assertEquals(pid, vm.uiState.value.pid)
        assertEquals(listOf(other), vm.uiState.value.credentials)
        collector.cancel()
    }

    @Test
    fun `a null user projects to an empty dashboard`() = runTest {
        val vm = DashboardViewModel(userRepository)
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        users.value = user(pid = credential("pid-1"), credentials = emptyList())
        users.value = null

        assertNull(vm.uiState.value.pid)
        assertEquals(emptyList<SavedCredential>(), vm.uiState.value.credentials)
        collector.cancel()
    }

    @Test
    fun `formatDate zero-pads the day and abbreviates the month`() {
        val date = Date.from(
            LocalDate.of(2026, 3, 9).atStartOfDay(ZoneId.systemDefault()).toInstant(),
        )

        val formatted = formatDate(date)

        // The pattern resolves month names against the default format locale,
        // so the expected month name has to come from the same locale.
        val month = Month.MARCH.getDisplayName(
            TextStyle.SHORT,
            Locale.getDefault(Locale.Category.FORMAT),
        )
        assertEquals("09 $month 2026", formatted)
    }
}
