@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("eu.tintera.background.android.library")
}
kotlin {

    compilerOptions {
        optIn.addAll(
            "kotlin.concurrent.atomics.ExperimentalAtomicApi"
        )
    }

    //iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm()

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
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(projects.db)
            implementation(projects.core)

            api(projects.api)
            api(libs.androidx.sqlite)
            api(projects.guard)
            implementation(projects.di)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)

        }

        androidMain.dependencies {
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.androidx.core.ktx)
            implementation(libs.koin.android)
            implementation(projects.android)
            implementation(projects.androidDb)
        }

        iosMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
            implementation(projects.ios)
            implementation(projects.iosDb)
        }

        jvmMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
            implementation(projects.engine)
            implementation(projects.engineDb)
        }

        webMain.dependencies {
            implementation(projects.engine)
            implementation(projects.engineDb)
            implementation(projects.web)
        }
    }
}