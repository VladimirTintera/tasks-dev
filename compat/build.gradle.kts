@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.serialization)
}
kotlin {

    jvmToolchain(11)

    androidTarget()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
        )

        optIn.addAll(
            "kotlin.uuid.ExperimentalUuidApi"
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
        androidMain.dependencies {
            implementation(projects.android)
            implementation(libs.androidx.work.runtime.ktx)
        }
        commonMain.dependencies {
            api(projects.api)
            implementation(libs.kotlinx.serialization.protobuf)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "eu.tintera.tasks"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}