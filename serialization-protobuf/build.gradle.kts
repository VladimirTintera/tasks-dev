@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.serialization)
    id("tasks.android.library")
}
kotlin {

    jvmToolchain(11)

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
        )

        optIn.addAll(
            "kotlin.uuid.ExperimentalUuidApi",
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
            api(projects.api)
            implementation(libs.kotlinx.serialization.protobuf)
        }
    }
}