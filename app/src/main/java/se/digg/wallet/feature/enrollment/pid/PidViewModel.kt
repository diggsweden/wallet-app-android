package se.digg.wallet.feature.enrollment.pid

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import se.digg.wallet.core.storage.user.DatabaseProvider
import se.digg.wallet.core.storage.user.UserRepository

class PidViewModel (private val repo: UserRepository) : ViewModel() {



    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = DatabaseProvider.get(appContext)
            val repo = UserRepository(db.userDao())
            return PidViewModel(repo) as T
        }
    }
}