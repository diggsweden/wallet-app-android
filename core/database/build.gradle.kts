// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

plugins {
    id("wallet.android.library")
    id("wallet.android.hilt")
    id("wallet.android.room")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "se.digg.wallet.core.database"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    sourceSets {
        getByName("test").assets.directories.add("$projectDir/schemas")
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }
}

dependencies {
    implementation(project(":core:model"))

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.room.testing)
}
