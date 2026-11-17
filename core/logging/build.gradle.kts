// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

plugins {
    id("wallet.android.library")
    id("wallet.android.hilt")
}

android {
    namespace = "se.digg.wallet.core.logging"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:network"))

    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.mock)
    testImplementation(libs.robolectric)
}

tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
    }
}
