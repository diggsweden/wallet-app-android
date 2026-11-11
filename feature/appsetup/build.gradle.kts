// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

plugins {
    id("wallet.android.feature")
}

android {
    namespace = "se.digg.wallet.feature.appsetup"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:trust"))

    testImplementation(project(":core:database"))
    testImplementation(libs.androidx.junit)
    testImplementation(libs.ktor.mock)
    testImplementation(libs.ktor.content.negotiation)
    testImplementation(libs.ktor.serialization)
}
