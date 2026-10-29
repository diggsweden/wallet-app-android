// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("wallet.android.library.compose")
                apply("wallet.android.hilt")
            }

            dependencies {
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:common"))
                "implementation"(project(":core:navigation"))
                "implementation"(project(":core:model"))

                "implementation"(libs.findLibrary("androidx-lifecycle-compose").get())
                "implementation"(libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
                "implementation"(libs.findLibrary("androidx-navigation3-runtime").get())
                "implementation"(libs.findLibrary("androidx-navigation3-ui").get())
                "implementation"(libs.findLibrary("androidx-lifecycle-viewmodel-navigation3").get())
                "implementation"(libs.findLibrary("timber").get())

                "testImplementation"(libs.findLibrary("junit").get())
                "testImplementation"(libs.findLibrary("kotlinx-coroutines-test").get())
            }
        }
    }
}
