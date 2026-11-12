// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

plugins {
    id("wallet.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "se.digg.wallet.feature.issuance"
}

dependencies {
    implementation(project(":core:crypto"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:trust"))
    implementation(project(":core:webauth"))

    implementation(libs.eudi.openid4vci)
    implementation(libs.eudi.sdjwt)
    implementation(libs.nimbus)
    implementation(libs.bundles.ktor)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.androidx.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.ktor.mock)
    testImplementation(libs.ktor.content.negotiation)
    testImplementation(libs.ktor.serialization)
    testImplementation(testFixtures(project(":core:trust")))
}
