// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

plugins {
    `kotlin-dsl`
}

group = "se.digg.wallet.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.kotlinter.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "wallet.android.library"
            implementationClass = "se.digg.wallet.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "wallet.android.library.compose"
            implementationClass = "se.digg.wallet.buildlogic.AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "wallet.android.feature"
            implementationClass = "se.digg.wallet.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("androidHilt") {
            id = "wallet.android.hilt"
            implementationClass = "se.digg.wallet.buildlogic.AndroidHiltConventionPlugin"
        }
        register("androidRoom") {
            id = "wallet.android.room"
            implementationClass = "se.digg.wallet.buildlogic.AndroidRoomConventionPlugin"
        }
    }
}
