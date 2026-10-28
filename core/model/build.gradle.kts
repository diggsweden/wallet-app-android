// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

plugins {
    id("wallet.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "se.digg.wallet.core.model"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.nimbus)

    testImplementation(libs.junit)
}
