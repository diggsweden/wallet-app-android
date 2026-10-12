// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinter)
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "se.digg.wallet.core.common"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlinter {
    ktlintVersion = libs.versions.ktlint.get()
}

tasks.withType<LintTask> {
    exclude { it.file.path.contains("/build/generated") }
}

tasks.withType<FormatTask> {
    exclude { it.file.path.contains("/build/generated") }
}
