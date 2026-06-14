@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("eu.tintera.background.android.library")
}
kotlin {

    compilerOptions {
        optIn.addAll(
            "kotlinx.serialization.ExperimentalSerializationApi"
        )
    }

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
        androidMain.dependencies {
            implementation(libs.androidx.work.runtime.ktx)
        }
        commonMain.dependencies {
            api(projects.tasks.api)
            api(projects.tasks.koin.koin)
            implementation(projects.tasks.serialization.protobuf)
        }
    }
}