package se.digg.wallet.feature.presentation

data class DisclosureUiModel(
    val id: String,
    val label: String,
    val value: String,
    val checked: Boolean = false
)