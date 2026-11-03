// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

plugins {
    id("wallet.android.library")
    id("wallet.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "se.digg.wallet.core.trust"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:network"))

    api(libs.nimbus)
    implementation(libs.timber)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.mock)
    testImplementation(libs.bouncycastle.pkix)
}

tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
    }
}
