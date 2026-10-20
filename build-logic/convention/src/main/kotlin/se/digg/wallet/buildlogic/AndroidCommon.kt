// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jmailen.gradle.kotlinter.KotlinterExtension
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureKotlinAndroid(
    libraryExtension: LibraryExtension,
) {
    libraryExtension.apply {
        compileSdk = 37

        defaultConfig {
            minSdk = 29
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    configure<KotlinAndroidProjectExtension> {
        jvmToolchain(17)
    }
}

internal fun Project.configureKotlinter() {
    val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion

    configure<KotlinterExtension> {
        this.ktlintVersion = ktlintVersion
    }

    tasks.withType<LintTask>().configureEach {
        exclude { it.file.path.contains("/build/generated") }
    }
    tasks.withType<FormatTask>().configureEach {
        exclude { it.file.path.contains("/build/generated") }
    }
}

internal fun Project.configureAndroidCompose(
    libraryExtension: LibraryExtension,
) {
    libraryExtension.apply {
        buildFeatures {
            compose = true
        }

        lint {
            abortOnError = false
            disable.add("UnusedMaterial3ScaffoldPaddingParameter")
        }
    }

    val ktlintCompose = libs.findLibrary("ktlint-compose").get()
    dependencies.add("ktlint", ktlintCompose)

    val composeBom = libs.findLibrary("androidx-compose-bom").get()
    dependencies.add("implementation", dependencies.platform(composeBom))
    dependencies.add("implementation", libs.findLibrary("androidx-compose-material3").get())
    dependencies.add("implementation", libs.findLibrary("androidx-ui").get())
    dependencies.add("implementation", libs.findLibrary("androidx-ui-graphics").get())
    dependencies.add("implementation", libs.findLibrary("androidx-ui-tooling-preview").get())
    dependencies.add("debugImplementation", libs.findLibrary("androidx-ui-tooling").get())
}
