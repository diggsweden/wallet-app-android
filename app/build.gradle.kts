// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

import ch.acanda.gradle.fabrikt.FabriktGenerateTask
import com.google.devtools.ksp.gradle.KspAATask
import java.util.Properties
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

val secretsProperties = Properties().apply {
    val secretsFile = rootProject.file("secrets.properties")
    if (secretsFile.exists()) {
        secretsFile.inputStream().use(::load)
    }
}

fun getSecret(name: String): String =
    secretsProperties.getProperty(name) ?: error("Missing secret: $name")

fun getVersionCode(): Int {
    System.getenv("CI") ?: return 1

    return providers.exec {
        commandLine("git", "rev-list", "--count", "origin/main")
    }.standardOutput.asText.get().trim().toInt()
}

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
    alias(libs.plugins.room)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.aboutlibrariesAndroid)
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "se.digg.wallet"
    compileSdk = 37

    // TODO this can be removed when eudi-libraries are removed.
    packaging {
        resources.excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
        // bcprov/bcutil/bcpkix-jdk18on all bundle their own copy of these META-INF files.
        resources.excludes.add("META-INF/LICENSE.md")
        resources.excludes.add("META-INF/NOTICE.md")
    }

    defaultConfig {
        applicationId = "se.digg.wallet"
        minSdk = 31
        targetSdk = 37
        versionCode = project.findProperty("versionCode")?.toString()?.toInt() ?: getVersionCode()
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
        debug {
            enableUnitTestCoverage = true
        }
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
        buildConfig = true
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
            buildConfigField("String", "API_KEY", "\"${getSecret("SANDBOX_API_KEY")}\"")
            buildConfigField(
                "String",
                "BASE_URL",
                "\"wallet.sandbox.digg.se/api\"",
            )
            buildConfigField(
                "String",
                "PID_ISSUER_URL",
                "\"wallet.sandbox.digg.se/pid-issuer\"",
            )
        }
        create("local") {
            dimension = "version"
            applicationIdSuffix = ".local"
            buildConfigField("String", "API_KEY", "\"${getSecret("LOCAL_API_KEY")}\"")
            buildConfigField(
                "String",
                "BASE_URL",
                "\"10.0.2.2/wallet-client-gateway\"",
            )
            buildConfigField(
                "String",
                "PID_ISSUER_URL",
                "\"10.0.2.2/pid-issuer\"",
            )
        }
    }

    sourceSets.named("main") {
        kotlin.directories += fabriktOutputDirectory.get().asFile.path
    }
    sourceSets.named("androidTest") {
        assets.directories.add("$projectDir/schemas")
    }
}
androidComponents {
    onVariants { variant ->
        if (variant.debuggable) {
            listOf("shared", variant.flavorName).forEach {
                variant.sources.res?.addStaticSourceDirectory("src/banner/$it/res")
            }
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core:designsystem"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.browser)
    implementation(libs.aboutlibraries.compose.m3)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.mock)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.leakcanary)

    implementation(libs.nimbus)
    implementation(libs.timber)
    implementation(libs.bundles.images)
    implementation(libs.bundles.eudi)
    implementation(libs.bundles.storage)
    implementation(libs.bundles.di)
    implementation(libs.bundles.ktor)
    implementation(libs.accessMechanism)

    ksp(libs.hilt.compiler)

    ksp(libs.room.compiler)
    androidTestImplementation(libs.room.testing)

    constraints {
        implementation("org.bouncycastle:bcprov-jdk18on") {
            version { require("1.85") }
            because(
                "CVE-2026-59650: DH agreement exponentiates unvalidated peer value, fixed in 1.85",
            )
        }
        implementation("org.bouncycastle:bcutil-jdk18on") {
            version { require("1.85") }
            because(
                "must match bcprov-jdk18on 1.85 - bcutil 1.83 duplicates classes bcprov 1.85 now provides",
            )
        }
        implementation("org.bouncycastle:bcpkix-jdk18on") {
            version { require("1.85") }
            because("must match bcprov-jdk18on 1.85 - keep the whole BC trio on one version")
        }
    }
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
        apiFile = file("src/main/openapi/client-gateway.yaml")
        outputDirectory = fabriktOutputDirectory
        basePackage = "se.wallet.client.gateway"
        addFileDisclaimer = enabled
        validationLibrary = NoValidation
        typeOverrides {
            uuid = String
            date = String
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
private val generatedCode = listOf(
    "se/wallet/client/gateway/client/**",
    "se/wallet/client/gateway/models/**",
    "**/Dagger*",
    "**/Hilt_*",
    "**/HiltWrapper_*",
    "**/*_HiltModules*",
    "**/*_Factory*",
    "hilt_aggregated_deps/**",
    "**/*_Impl*",
    "**/*ComposableSingletons*",
)

private val diWiring = listOf(
    "se/digg/wallet/core/di/RepositoryModule*",
    "se/digg/wallet/core/di/DatabaseModule*",
)
private val composeUi = listOf(
    "**/*ScreenKt.class",
    "**/*ScreenKt$*.class",
    "se/digg/wallet/core/designsystem/**",
    "**/WalletNavHostKt*",
    "**/MainActivity.class",
    "**/MainActivity$*.class",
    "**/MainActivityKt*",
)

private val allSourceDirectories
    get() = files(
        "src/main/java",
        fabriktOutputDirectory.map { it.dir("src/main/kotlin") },
        layout.buildDirectory.dir("generated/ksp/demoDebug/java"),
        layout.buildDirectory.dir("generated/ksp/demoDebug/kotlin"),
        layout.buildDirectory.dir("generated/hilt/component_sources/demoDebug"),
        layout.buildDirectory.dir("generated/ap_generated_sources/demoDebug/out"),
    )

private val allClassDirectories
    get() = files(
        layout.buildDirectory.dir(
            "intermediates/built_in_kotlinc/demoDebug/compileDemoDebugKotlin/classes",
        ),
        layout.buildDirectory.dir(
            "intermediates/javac/demoDebug/compileDemoDebugJavaWithJavac/classes",
        ),
        // Hilt's generated component tree is compiled by its own task, outside javac's output.
        layout.buildDirectory.dir("intermediates/classes/demoDebug/hiltJavaCompileDemoDebug"),
    )

tasks.register<JacocoReport>("jacocoTestReportAll") {
    group = "verification"
    description =
        "Unfiltered coverage for the demoDebug unit tests, including generated code and Compose UI."
    dependsOn("testDemoDebugUnitTest")

    reports {
        html.required = true
        xml.required = true
        csv.required = false
    }
    classDirectories.setFrom(allClassDirectories.asFileTree)
    sourceDirectories.setFrom(allSourceDirectories)
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("outputs/unit_test_code_coverage/demoDebugUnitTest/*.exec")
        },
    )
}

tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description =
        "Coverage for the demoDebug unit tests, excluding DI/Room/Ktor-client generated code and Compose UI."
    dependsOn("testDemoDebugUnitTest")

    reports {
        html.required = true
        xml.required = true
        csv.required = false
    }
    classDirectories.setFrom(
        files(
            layout.buildDirectory.dir(
                "intermediates/built_in_kotlinc/demoDebug/compileDemoDebugKotlin/classes",
            ),
            layout.buildDirectory.dir(
                "intermediates/javac/demoDebug/compileDemoDebugJavaWithJavac/classes",
            ),
        ).asFileTree.matching { exclude(generatedCode + composeUi + diWiring) },
    )
    sourceDirectories.setFrom(
        files(
            "src/main/java",
            fabriktOutputDirectory.map { it.dir("src/main/kotlin") },
        ),
    )
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("outputs/unit_test_code_coverage/demoDebugUnitTest/*.exec")
        },
    )
}
