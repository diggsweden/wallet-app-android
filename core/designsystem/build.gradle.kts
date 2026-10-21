// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

plugins {
    id("wallet.android.library.compose")
}

android {
    namespace = "se.digg.wallet.core.designsystem"
}

dependencies {
    implementation(libs.bundles.images)
}
