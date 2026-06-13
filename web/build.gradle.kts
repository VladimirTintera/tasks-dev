@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("eu.tintera.tasks.kmp.library")
}

kotlin {

    compilerOptions {
        optIn.addAll(
            "kotlin.concurrent.atomics.ExperimentalAtomicApi",
        )
    }

    js {
        browser()
        useEsModules()
    }

    wasmJs {
        browser()
        useEsModules()
    }


    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.sqlite.web)
            implementation(npm("sqlite-wasm-worker", layout.projectDirectory.dir("worker").asFile))
        }

        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
    }
}