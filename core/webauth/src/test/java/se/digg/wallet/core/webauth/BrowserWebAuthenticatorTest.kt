// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.webauth

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.browser.auth.AuthResultTestFactory
import androidx.browser.auth.AuthTabIntent
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class BrowserWebAuthenticatorTest {

    private lateinit var context: Context
    private lateinit var authenticator: BrowserWebAuthenticator

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        authenticator = BrowserWebAuthenticator(context)
    }

    @Test
    fun `onLaunchFailed completes authenticate with failure`() = runTest {
        val authDeferred = async {
            authenticator.authenticate("https://example.com/auth", "wallet-app")
        }
        testScheduler.runCurrent()

        authenticator.onLaunchFailed(RuntimeException("Browser unavailable"))

        val result = authDeferred.await()
        assertTrue(result is WebAuthResult.Failure)
        assertEquals("Could not open the browser", (result as WebAuthResult.Failure).message)
    }

    @Test
    fun `onDeepLink with matching scheme completes authenticate with success`() = runTest {
        val authDeferred = async {
            authenticator.authenticate("https://example.com/auth", "wallet-app")
        }
        testScheduler.runCurrent()

        val uri = Uri.parse("wallet-app://callback?code=abc123state")
        val handled = authenticator.onDeepLink(uri)

        assertTrue(handled)
        val result = authDeferred.await()
        assertTrue(result is WebAuthResult.Success)
        assertEquals(
            "wallet-app://callback?code=abc123state",
            (result as WebAuthResult.Success).callbackUri,
        )
    }

    @Test
    fun `onDeepLink with mixed-case scheme completes authenticate with success`() = runTest {
        val authDeferred = async {
            authenticator.authenticate("https://example.com/auth", "wallet-app")
        }
        testScheduler.runCurrent()

        val uri = Uri.parse("WALLET-APP://callback?code=abc123state")
        val handled = authenticator.onDeepLink(uri)

        assertTrue(handled)
        val result = authDeferred.await()
        assertTrue(result is WebAuthResult.Success)
        assertEquals(
            "WALLET-APP://callback?code=abc123state",
            (result as WebAuthResult.Success).callbackUri,
        )
    }

    @Test
    fun `onDeepLink with mismatching scheme returns false and does not complete`() = runTest {
        val authDeferred = async {
            authenticator.authenticate("https://example.com/auth", "wallet-app")
        }
        testScheduler.runCurrent()

        val mismatchUri = Uri.parse("other-app://callback?code=abc")
        val handled = authenticator.onDeepLink(mismatchUri)

        assertFalse(handled)
        assertFalse(authDeferred.isCompleted)

        // Clean up session
        authenticator.onLaunchFailed(Exception("cancel"))
        authDeferred.await()
    }

    @Test
    fun `onDeepLink returns false when pendingSession is null`() {
        val handled = authenticator.onDeepLink(Uri.parse("wallet-app://callback"))
        assertFalse(handled)
    }

    @Test
    fun `onDeepLink when active session is AuthTab returns false and does not complete`() =
        runTest {
            registerBrowser(
                packageName = "com.android.chrome",
                supportsCustomTabs = true,
                supportsAuthTab = true,
            )
            val authDeferred = async {
                authenticator.authenticate("https://example.com/auth", "wallet-app")
            }
            testScheduler.runCurrent()

            val handled = authenticator.onDeepLink(Uri.parse("wallet-app://callback?code=abc"))
            assertFalse(handled)
            assertFalse(authDeferred.isCompleted)

            authenticator.onLaunchFailed(Exception("cleanup"))
            authDeferred.await()
        }

    @Test
    fun `onHostPaused followed by onHostResumed cancels session when browser was shown`() =
        runTest {
            val authDeferred = async {
                authenticator.authenticate("https://example.com/auth", "wallet-app")
            }
            testScheduler.runCurrent()

            authenticator.onHostPaused()
            authenticator.onHostResumed()

            val result = authDeferred.await()
            assertEquals(WebAuthResult.Cancelled, result)
        }

    @Test
    fun `onHostResumed before onHostPaused does not cancel session`() = runTest {
        val authDeferred = async {
            authenticator.authenticate("https://example.com/auth", "wallet-app")
        }
        testScheduler.runCurrent()

        authenticator.onHostResumed()
        assertFalse(authDeferred.isCompleted)

        authenticator.onLaunchFailed(Exception("cleanup"))
        authDeferred.await()
    }

    @Test
    fun `onHostResumed does not cancel session when active request is AuthTab`() = runTest {
        registerBrowser(
            packageName = "com.android.chrome",
            supportsCustomTabs = true,
            supportsAuthTab = true,
        )
        val authDeferred = async {
            authenticator.authenticate("https://example.com/auth", "wallet-app")
        }
        testScheduler.runCurrent()

        authenticator.onHostPaused()
        authenticator.onHostResumed()

        assertFalse(authDeferred.isCompleted)

        authenticator.onLaunchFailed(Exception("cleanup"))
        authDeferred.await()
    }

    @Test
    fun `onHostResumed and onHostPaused without active session do not crash`() {
        authenticator.onHostPaused()
        authenticator.onHostResumed()
    }

    @Test
    fun `subsequent authenticate cancels previous pending session`() = runTest {
        val firstAuth = async {
            authenticator.authenticate("https://example.com/first", "wallet-app")
        }
        testScheduler.runCurrent()

        val secondAuth = async {
            authenticator.authenticate("https://example.com/second", "wallet-app")
        }
        testScheduler.runCurrent()

        val firstResult = firstAuth.await()
        assertEquals(WebAuthResult.Cancelled, firstResult)

        authenticator.onLaunchFailed(Exception("end second"))
        val secondResult = secondAuth.await()
        assertTrue(secondResult is WebAuthResult.Failure)
    }

    @Test
    fun `onAuthTabResult with RESULT_OK and valid uri completes authenticate with success`() =
        runTest {
            val authDeferred = async {
                authenticator.authenticate("https://example.com/auth", "wallet-app")
            }
            testScheduler.runCurrent()

            val result = AuthResultTestFactory.create(
                AuthTabIntent.RESULT_OK,
                Uri.parse("wallet-app://callback?code=authtab123"),
            )
            authenticator.onAuthTabResult(result)

            val authResult = authDeferred.await()
            assertTrue(authResult is WebAuthResult.Success)
            assertEquals(
                "wallet-app://callback?code=authtab123",
                (authResult as WebAuthResult.Success).callbackUri,
            )
        }

    @Test
    fun `onAuthTabResult with RESULT_OK and null uri completes with missing uri failure`() =
        runTest {
            val authDeferred = async {
                authenticator.authenticate("https://example.com/auth", "wallet-app")
            }
            testScheduler.runCurrent()

            val result = AuthResultTestFactory.create(AuthTabIntent.RESULT_OK, null)
            authenticator.onAuthTabResult(result)

            val authResult = authDeferred.await()
            assertTrue(authResult is WebAuthResult.Failure)
            assertEquals("Missing callback URI", (authResult as WebAuthResult.Failure).message)
        }

    @Test
    fun `onAuthTabResult with RESULT_CANCELED completes authenticate with cancelled`() = runTest {
        val authDeferred = async {
            authenticator.authenticate("https://example.com/auth", "wallet-app")
        }
        testScheduler.runCurrent()

        val result = AuthResultTestFactory.create(AuthTabIntent.RESULT_CANCELED, null)
        authenticator.onAuthTabResult(result)

        val authResult = authDeferred.await()
        assertEquals(WebAuthResult.Cancelled, authResult)
    }

    @Test
    fun `onAuthTabResult with verification failures completes with verification failed`() =
        runTest {
            val authDeferred1 = async {
                authenticator.authenticate("https://example.com/auth", "wallet-app")
            }
            testScheduler.runCurrent()

            authenticator.onAuthTabResult(
                AuthResultTestFactory.create(AuthTabIntent.RESULT_VERIFICATION_FAILED, null),
            )
            val authResult1 = authDeferred1.await()
            assertTrue(authResult1 is WebAuthResult.Failure)
            assertEquals("Verification failed", (authResult1 as WebAuthResult.Failure).message)

            val authDeferred2 = async {
                authenticator.authenticate("https://example.com/auth", "wallet-app")
            }
            testScheduler.runCurrent()

            authenticator.onAuthTabResult(
                AuthResultTestFactory.create(
                    AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT,
                    null,
                ),
            )
            val authResult2 = authDeferred2.await()
            assertTrue(authResult2 is WebAuthResult.Failure)
            assertEquals("Verification failed", (authResult2 as WebAuthResult.Failure).message)
        }

    @Test
    fun `onAuthTabResult with unknown code completes with unknown error failure`() = runTest {
        val authDeferred = async {
            authenticator.authenticate("https://example.com/auth", "wallet-app")
        }
        testScheduler.runCurrent()

        val result = AuthResultTestFactory.create(9999, null)
        authenticator.onAuthTabResult(result)

        val authResult = authDeferred.await()
        assertTrue(authResult is WebAuthResult.Failure)
        assertEquals("Unknown error", (authResult as WebAuthResult.Failure).message)
    }

    @Test
    fun `onAuthTabResult without active session does not throw`() {
        val result = AuthResultTestFactory.create(
            AuthTabIntent.RESULT_OK,
            Uri.parse("wallet-app://cb"),
        )
        authenticator.onAuthTabResult(result)
    }

    @Test
    fun `authenticate emits External request when no custom tabs provider exists`() = runTest {
        val authDeferred = async {
            authenticator.authenticate("https://example.com/auth", "wallet-app")
        }
        testScheduler.runCurrent()

        val request = authenticator.browserRequests.first()
        assertTrue(request is BrowserRequest.External)
        assertEquals(Uri.parse("https://example.com/auth"), request.url)

        authenticator.onLaunchFailed(Exception("cleanup"))
        authDeferred.await()
    }

    @Test
    fun `authenticate emits CustomTab request when provider does not support auth tab`() = runTest {
        registerBrowser(
            packageName = "com.android.chrome",
            supportsCustomTabs = true,
            supportsAuthTab = false,
        )
        val authDeferred = async {
            authenticator.authenticate("https://example.com/auth", "wallet-app")
        }
        testScheduler.runCurrent()

        val request = authenticator.browserRequests.first()
        assertTrue(request is BrowserRequest.CustomTab)
        assertEquals(Uri.parse("https://example.com/auth"), request.url)

        authenticator.onLaunchFailed(Exception("cleanup"))
        authDeferred.await()
    }

    @Test
    fun `authenticate emits AuthTab request when provider supports auth tab`() = runTest {
        registerBrowser(
            packageName = "com.android.chrome",
            supportsCustomTabs = true,
            supportsAuthTab = true,
        )
        val authDeferred = async {
            authenticator.authenticate("https://example.com/auth", "wallet-app")
        }
        testScheduler.runCurrent()

        val request = authenticator.browserRequests.first()
        assertTrue(request is BrowserRequest.AuthTab)
        assertEquals("wallet-app", (request as BrowserRequest.AuthTab).callbackScheme)
        assertEquals(Uri.parse("https://example.com/auth"), request.url)

        authenticator.onLaunchFailed(Exception("cleanup"))
        authDeferred.await()
    }

    private fun registerBrowser(
        packageName: String,
        supportsCustomTabs: Boolean,
        supportsAuthTab: Boolean,
    ) {
        val shadowPackageManager = Shadows.shadowOf(context.packageManager)

        val activityResolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                this.packageName = packageName
                name = "$packageName.MainActivity"
            }
        }
        @Suppress("DEPRECATION")
        shadowPackageManager.addResolveInfoForIntent(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
            activityResolveInfo,
        )

        if (supportsCustomTabs) {
            val serviceResolveInfo = ResolveInfo().apply {
                serviceInfo = ServiceInfo().apply {
                    this.packageName = packageName
                    name = "$packageName.CustomTabsService"
                }
                if (supportsAuthTab) {
                    filter = IntentFilter().apply {
                        addCategory("androidx.browser.auth.category.AuthTab")
                    }
                }
            }
            @Suppress("DEPRECATION")
            shadowPackageManager.addResolveInfoForIntent(
                Intent("android.support.customtabs.action.CustomTabsService"),
                serviceResolveInfo,
            )
            @Suppress("DEPRECATION")
            shadowPackageManager.addResolveInfoForIntent(
                Intent(
                    "android.support.customtabs.action.CustomTabsService",
                ).setPackage(packageName),
                serviceResolveInfo,
            )
        }
    }
}
