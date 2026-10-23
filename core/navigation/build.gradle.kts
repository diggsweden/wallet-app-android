// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

plugins {
    id("wallet.android.library.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "se.digg.wallet.core.navigation"

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
