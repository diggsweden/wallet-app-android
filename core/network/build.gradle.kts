// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

import ch.acanda.gradle.fabrikt.FabriktGenerateTask
import com.google.devtools.ksp.gradle.KspAATask

val fabriktGenerateTask = tasks.named<FabriktGenerateTask>("fabriktGenerate")
val fabriktOutputDirectory =
    layout.buildDirectory.dir("generated/sources/fabrikt/src/main")

plugins {
    id("wallet.android.library")
    id("wallet.android.hilt")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.fabrikt)
}

android {
    namespace = "se.digg.wallet.core.network"

    sourceSets {
        getByName("main") {
            kotlin.directories += fabriktOutputDirectory.get().asFile.path
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:crypto"))

    api(libs.bundles.ktor)
    api(libs.kotlinx.serialization.json)
    implementation(libs.nimbus)
    implementation(libs.accessMechanism)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.mock)
}

tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
    }
}

fabrikt {
    generate("client-gateway") {
        apiFile = file("src/main/openapi/client-gateway.yaml")
        outputDirectory = fabriktOutputDirectory
        basePackage = "se.wallet.client.gateway"
        addFileDisclaimer = enabled
        validationLibrary = NoValidation
        typeOverrides {
            uuid = String
            date = String
            datetime = String
            uri = String
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
