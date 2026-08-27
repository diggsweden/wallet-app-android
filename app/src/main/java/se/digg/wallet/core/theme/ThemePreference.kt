// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.theme

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeOption { SYSTEM, LIGHT, DARK }

private const val PREFS_NAME = "theme_prefs"
private const val KEY_THEME_OPTION = "theme_option"

// A color scheme swap only needs a recomposition, not an Activity recreate, so this is observed
// reactively instead of being re-read on relaunch the way LocaleOverride is.
object ThemePreference {
    private val _option = MutableStateFlow(ThemeOption.SYSTEM)
    val option: StateFlow<ThemeOption> = _option

    fun init(context: Context) {
        _option.value = readStored(context)
    }

    fun set(context: Context, option: ThemeOption) {
        prefs(context).edit { putString(KEY_THEME_OPTION, option.name) }
        _option.value = option
    }

    private fun readStored(context: Context): ThemeOption {
        val stored = prefs(context).getString(KEY_THEME_OPTION, null) ?: return ThemeOption.SYSTEM
        return runCatching { ThemeOption.valueOf(stored) }.getOrDefault(ThemeOption.SYSTEM)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
