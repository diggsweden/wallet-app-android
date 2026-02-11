package se.digg.wallet.feature.enrollment

data class EnrollmentUiState(
    val currentStep: EnrollmentStep = EnrollmentStep.NOTIFICATION,
    val totalSteps: Int = EnrollmentStep.totalSteps,
    val enableBack: List<EnrollmentStep> =
        listOf(
            EnrollmentStep.VERIFY_EMAIL,
            EnrollmentStep.VERIFY_PHONE,
            EnrollmentStep.VERIFY_PIN,
            EnrollmentStep.CREDENTIAL_OFFER,
        ),
)

sealed interface EnrollmentUiEvent {
    data object LocalStorageCleared : EnrollmentUiEvent
}

sealed interface EnrollmentUiEffect {
    object OnNext : EnrollmentUiEffect
}

enum class EnrollmentStep(val stepTitle: String) {
    NOTIFICATION("Notification"),
    LOGIN("Login"),
    PHONE_NUMBER("Phone number"),
    VERIFY_PHONE("Verify phone"),
    EMAIL("Email"),
    VERIFY_EMAIL("Verify email"),
    PIN("PIN"),
    VERIFY_PIN("Verify PIN"),
    FETCH_PID("PID"),
    CREDENTIAL_OFFER("Credential offer"),
    ;

    companion object {
        val totalSteps: Int get() = entries.size
    }
}
