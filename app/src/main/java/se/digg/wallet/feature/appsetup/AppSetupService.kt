// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.appsetup

import javax.inject.Inject
import se.digg.wallet.core.trust.TrustStore
import se.digg.wallet.data.UserRepository

enum class StartDestination { Intro, Reset, Dashboard }

interface AppSetupService {
    suspend fun resolveStartDestination(): StartDestination
    suspend fun clearUserData()
}

internal class DefaultAppSetupService @Inject constructor(
    private val userRepository: UserRepository,
) : AppSetupService {

    override suspend fun resolveStartDestination(): StartDestination {
        val user = userRepository.getUser() ?: return StartDestination.Intro
        return when {
            user.isReset -> StartDestination.Reset

            !user.isOnboarded -> StartDestination.Intro

            isBackendGenerationOutdated(
                localGeneration = user.backendGeneration,
            ) -> StartDestination.Reset

            else -> StartDestination.Dashboard
        }
    }

    override suspend fun clearUserData() = userRepository.wipeAll()

    private suspend fun isBackendGenerationOutdated(localGeneration: Int?): Boolean {
        val backendGeneration = userRepository.getClientConfig().cacheGeneration

        if (localGeneration == null) {
            userRepository.setBackendGeneration(backendGeneration)
            return false
        }

        return backendGeneration > localGeneration
    }
}
