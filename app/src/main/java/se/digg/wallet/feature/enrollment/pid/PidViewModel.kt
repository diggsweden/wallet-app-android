package se.digg.wallet.feature.enrollment.pid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PidViewModel@Inject constructor(private val userRepository: se.digg.wallet.data.UserRepository) :
    ViewModel() {
    val credential = userRepository.user.map { it?.credential }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}