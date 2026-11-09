// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

plugins {
    id("wallet.android.library")
    id("wallet.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "se.digg.wallet.core.data"

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    api(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:crypto"))

    api(libs.accessMechanism)
    api(libs.bundles.eudi)
    api(libs.nimbus)
    implementation(libs.bundles.ktor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.mock)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.testing)
}
