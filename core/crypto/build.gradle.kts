// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

plugins {
    id("wallet.android.library")
    id("wallet.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "se.digg.wallet.core.crypto"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.nimbus)
    api(libs.eudi.openid4vci)
    implementation(libs.bouncycastle.pkix)
    implementation(libs.timber)
    implementation(libs.accessMechanism)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
    }
}
