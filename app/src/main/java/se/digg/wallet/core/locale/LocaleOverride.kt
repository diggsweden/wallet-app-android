// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.locale

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.core.content.edit
import java.util.Locale

private const val PREFS_NAME = "locale_prefs"
private const val KEY_LANGUAGE_TAG = "language_tag"

// Manual per-app locale override: MainActivity is a plain ComponentActivity, so the
// AppCompat/framework per-app-language APIs (which require an active AppCompatDelegate) have
// no effect here. This wraps the base Context with the user's chosen locale instead.
object LocaleOverride {
    fun wrap(context: Context): Context {
        val locale = languageTag(context)
            ?.let { Locale.forLanguageTag(it) }
            ?: Resources.getSystem().configuration.locales[0]
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun languageTag(context: Context): String? = prefs(context).getString(KEY_LANGUAGE_TAG, null)

    fun setLanguageTag(context: Context, languageTag: String?) {
        prefs(context).edit { putString(KEY_LANGUAGE_TAG, languageTag) }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
