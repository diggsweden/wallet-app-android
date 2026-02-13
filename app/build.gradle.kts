// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

import ch.acanda.gradle.fabrikt.FabriktGenerateTask
import com.google.devtools.ksp.gradle.KspAATask
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

val fabriktGenerateTask = tasks.named<FabriktGenerateTask>("fabriktGenerate")
val fabriktOutputDirectory =
    layout.buildDirectory.dir("generated/sources/fabrikt/src/main")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.fabrikt)
    alias(libs.plugins.kotlinter)
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "se.digg.wallet"
    compileSdk = 36

    // TODO this can be removed when eudi-libraries are removed.
    packaging {
        resources.excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    }

    defaultConfig {
        applicationId = "se.digg.wallet"
        minSdk = 28
        targetSdk = 36
        versionCode = project.findProperty("versionCode")?.toString()?.toInt() ?: 1
        versionName = project.findProperty("versionName")?.toString() ?: "0.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Signing configuration for CI/CD - reads from environment variables
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
            if (keystorePath != null && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: ""
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Use release signing config if available (CI), otherwise use debug
            signingConfig = if (System.getenv("ANDROID_KEYSTORE_PATH") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
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
    flavorDimensions += listOf("version")
    productFlavors {
        create("demo") {
            dimension = "version"
            applicationIdSuffix = ".demo"
        }
    }

    sourceSets.named("main") {
        kotlin.directories += fabriktOutputDirectory.get().asFile.path
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.browser)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.timber)
    implementation(libs.bundles.images)
    implementation(libs.bundles.eudi)
    implementation(libs.bundles.storage)
    implementation(libs.bundles.di)
    ksp(libs.hilt.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
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

fabrikt {
    generate("client-gateway") {
        apiFile = file("src/main/openapi/client-gateway.json")
        outputDirectory = fabriktOutputDirectory
        basePackage = "se.wallet.client.gateway"
        addFileDisclaimer = enabled
        validationLibrary = NoValidation
        typeOverrides {
            uuid = String
        }
        client {
            generate = enabled
            target = Ktor
        }
        model {
            serializationLibrary = Kotlinx
        }
    }
}

tasks.withType<KspAATask>().configureEach {
    dependsOn(fabriktGenerateTask)
}

tasks.named("preBuild") {
    dependsOn(fabriktGenerateTask)
}

hilt {
    enableAggregatingTask = false
}
