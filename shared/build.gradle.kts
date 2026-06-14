@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialization)
    id("eu.tintera.background.android.library")
}

kotlin {

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters"
        )
        optIn.addAll(
            "kotlin.concurrent.atomics.ExperimentalAtomicApi",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true

            linkerOpts.add("-lsqlite3")
        }
    }

    jvm()

    js {
        browser()
    }

    wasmJs {
        browser()
    }

    sourceSets {

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.kermit)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(projects.runtime)
            implementation(projects.koinJson)
            implementation(projects.serializationCompat)
            implementation(projects.serializationJson)

            implementation(libs.time.format)
            implementation(libs.time.format.context)

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

androidLibrary.configure {
    androidResources { enable = true }
}