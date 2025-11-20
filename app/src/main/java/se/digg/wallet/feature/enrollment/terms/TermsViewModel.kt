package se.digg.wallet.feature.enrollment.terms

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import se.digg.wallet.data.UserRepository
import javax.inject.Inject

@HiltViewModel
class TermsViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

}