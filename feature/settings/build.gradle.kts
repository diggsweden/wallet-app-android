// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

plugins {
    id("wallet.android.feature")
}

android {
    namespace = "se.digg.wallet.feature.settings"
}

dependencies {
    implementation(project(":core:data"))
    implementation(libs.androidx.browser)
    implementation(libs.aboutlibraries.compose.m3)

    testImplementation(libs.androidx.junit)
    testImplementation(libs.turbine)
}
