// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

plugins {
    id("wallet.android.feature")
}

android {
    namespace = "se.digg.wallet.feature.qrscanner"
}

dependencies {
    implementation(project(":core:camera"))

    testImplementation(libs.androidx.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
}
