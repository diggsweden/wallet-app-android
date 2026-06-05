// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinter)
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "se.digg.wallet.core.designsystem"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        abortOnError = false
        disable.add("UnusedMaterial3ScaffoldPaddingParameter")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.bundles.images)
}

kotlinter {
    ktlintVersion = libs.versions.ktlint.get()
    dependencies {
        ktlint(libs.ktlint.compose)
    }
}

tasks.withType<LintTask> {
    exclude { it.file.path.contains("/build/generated") }
}

tasks.withType<FormatTask> {
    exclude { it.file.path.contains("/build/generated") }
}
