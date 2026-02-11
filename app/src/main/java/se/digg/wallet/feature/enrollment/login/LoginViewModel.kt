package se.digg.wallet.feature.enrollment.login

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import se.digg.wallet.core.oauth.LaunchAuthTab
import se.digg.wallet.core.oauth.OAuthCoordinator

sealed interface LoginUiEffect {
    data class OnLoginSuccessful(val sessionId: String) : LoginUiEffect
}

@HiltViewModel
class LoginViewModel @Inject constructor(private val oAuthCoordinator: OAuthCoordinator) :
    ViewModel() {

    private val _effects = MutableSharedFlow<LoginUiEffect>()
    val effects: SharedFlow<LoginUiEffect> = _effects.asSharedFlow()

    fun authorize(launchAuthTab: LaunchAuthTab) {
        viewModelScope.launch {
            val oAuthCallback = oAuthCoordinator.authorize(
                url = "https://wallet.sandbox.digg.se/api/oidc/auth".toUri(),
                redirectScheme = "wallet-app",
                launchAuthTab = launchAuthTab,
            )
            val sessionId =
                oAuthCallback.getQueryParameter("session_id")
                    ?: throw IllegalStateException("sessionId query parameter missing")
            _effects.emit(LoginUiEffect.OnLoginSuccessful(sessionId))
        }
    }
}
