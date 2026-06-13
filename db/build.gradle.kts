@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.serialization)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.ksp)
    id("eu.tintera.background.android.library")
}

room3 {
    schemaDirectory("$projectDir/schemas")
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
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.protobuf)

            api(libs.androidx.room.runtime)
            implementation(projects.core)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
    }
}

dependencies {
    with(libs.androidx.room.compiler) {
        add("kspAndroid", this)
        add("kspIosArm64", this)
        add("kspIosSimulatorArm64", this)
        add("kspJvm", this)
        add("kspJs", this)
        add("kspWasmJs", this)
    }
}