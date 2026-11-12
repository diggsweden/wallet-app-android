// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

plugins {
    id("wallet.android.feature")
}

android {
    namespace = "se.digg.wallet.feature.presentation"
}

dependencies {
    implementation(project(":core:crypto"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:trust"))

    implementation(libs.eudi.openid4vp)
    implementation(libs.eudi.sdjwt)
    implementation(libs.nimbus)
    implementation(libs.accessMechanism)
    implementation(libs.bundles.ktor)

    testImplementation(libs.androidx.junit)
    testImplementation(libs.turbine)
    testImplementation(testFixtures(project(":core:trust")))
}
