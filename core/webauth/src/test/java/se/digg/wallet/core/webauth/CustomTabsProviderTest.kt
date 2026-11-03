// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.webauth

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class CustomTabsProviderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `getCustomTabsProvider returns null when no browser is installed`() {
        assertNull(getCustomTabsProvider(context))
    }

    @Test
    fun `getCustomTabsProvider returns null when browser lacks custom tabs service`() {
        val shadowPackageManager = Shadows.shadowOf(context.packageManager)
        val browserPackage = "com.example.plainbrowser"

        val activityResolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = browserPackage
                name = "$browserPackage.MainActivity"
            }
        }
        shadowPackageManager.addResolveInfoForIntent(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
            activityResolveInfo,
        )

        assertNull(getCustomTabsProvider(context))
    }

    @Test
    fun `getCustomTabsProvider returns package name when custom tabs service is present`() {
        val shadowPackageManager = Shadows.shadowOf(context.packageManager)
        val browserPackage = "com.android.chrome"

        val activityResolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = browserPackage
                name = "$browserPackage.MainActivity"
            }
        }
        shadowPackageManager.addResolveInfoForIntent(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
            activityResolveInfo,
        )

        val serviceResolveInfo = ResolveInfo().apply {
            serviceInfo = ServiceInfo().apply {
                packageName = browserPackage
                name = "$browserPackage.CustomTabsService"
            }
        }
        shadowPackageManager.addResolveInfoForIntent(
            Intent(
                "android.support.customtabs.action.CustomTabsService",
            ).setPackage(browserPackage),
            serviceResolveInfo,
        )

        assertEquals(browserPackage, getCustomTabsProvider(context))
    }
}
