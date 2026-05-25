// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.registerpin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import se.digg.wallet.access_mechanism.api.OpaqueClient
import se.digg.wallet.core.di.BaseHttpClient
import se.digg.wallet.core.network.LocalOpaqueClient
import se.digg.wallet.core.services.KeystoreManager
import timber.log.Timber

private const val PIN = "12345"

@HiltViewModel
class RegisterPinViewModel @Inject constructor(
    @param:BaseHttpClient private val httpClient: HttpClient,
) : ViewModel() {

    private var opaqueApi: OpaqueClient? = null

    fun registerNewState() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val keyPair = KeystoreManager.createSoftwareEcdhKey()
                val keyPin = KeystoreManager.getPinStretchPrivateKey()

                opaqueApi = OpaqueClient.create(
                    clientKeyPair = keyPair,
                    pinStretchPrivateKey = keyPin,
                    transport = LocalOpaqueClient(httpClient),
                )
                Timber.d("RegisterPinViewModel - RegisterNewState successful")
            } catch (e: Exception) {
                Timber.d("RegisterPinViewModel - RegisterNewState error: ${e.message}")
            }
        }
    }

    fun registerPin() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val exportKey = checkNotNull(opaqueApi).registration(pin = PIN)
                checkNotNull(opaqueApi).authenticate(pin = PIN)
                Timber.d("RegisterPinViewModel - RegisterPin successful: $exportKey")
            } catch (e: Exception) {
                Timber.d("RegisterPinViewModel - RegisterPin error: ${e.message}")
            }
        }
    }

    fun changePin() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val changePinResult = checkNotNull(opaqueApi).changePin(
                    newPin = PIN,
                )
                Timber.d("RegisterPinViewModel - ChangePin successful: $changePinResult")
            } catch (e: Exception) {
                Timber.d("RegisterPinViewModel: ChangePin error: ${e.message}")
            }
        }
    }

    fun createHsmKey() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val createHsmKeyResult = checkNotNull(opaqueApi).createHsmKey()
                Timber.d("RegisterPinViewModel - CreateHsmKey successful: $createHsmKeyResult")
            } catch (e: Exception) {
                Timber.d("RegisterPinViewModel: CreateHsmKey error: ${e.message}")
            }
        }
    }

    fun listHsmKeys() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val listHsmKeysResult = checkNotNull(opaqueApi).listHsmKeys()
                Timber.d("RegisterPinViewModel - ListHsmKeys successful: $listHsmKeysResult")
            } catch (e: Exception) {
                Timber.d("RegisterPinViewModel: ListHsmKeys error: ${e.message}")
            }
        }
    }
}
