@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("eu.tintera.background.android.library")
}
kotlin {

    compilerOptions {
        optIn.addAll(
            "kotlin.concurrent.atomics.ExperimentalAtomicApi",
        )
    }

    //iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm()

    js {
        browser()
    }

    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            api(projects.tasks.api)
            api(projects.tasks.core.core)
            implementation(projects.tasks.core.db)
            implementation(libs.koin.core)
            api(projects.guard)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}