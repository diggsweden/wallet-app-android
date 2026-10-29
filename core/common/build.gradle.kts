// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

plugins {
    id("wallet.android.library")
    id("wallet.android.hilt")
}

android {
    namespace = "se.digg.wallet.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
