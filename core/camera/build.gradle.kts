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
    namespace = "se.digg.wallet.core.camera"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.camera)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
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
