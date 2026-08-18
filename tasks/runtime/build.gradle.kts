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
            implementation(projects.tasks.db)
            implementation(projects.tasks.core.core)

            api(projects.tasks.api)
            api(libs.androidx.sqlite)
            api(projects.guard)
            implementation(projects.tasks.di)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)

        }

        androidMain.dependencies {
            implementation(libs.androidx.work.runtime.ktx)
            // TaskManagerStartupInitializer implements androidx.startup.Initializer and the manifest
            // registers InitializationProvider — declared directly rather than relying on WorkManager.
            api(libs.androidx.startup.runtime)
            implementation(libs.androidx.core.ktx)
            implementation(libs.koin.android)
            implementation(projects.tasks.android.android)
            implementation(projects.tasks.android.db)
        }

        iosMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
            implementation(projects.tasks.ios.ios)
            implementation(projects.tasks.ios.db)
        }

        jvmMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
            implementation(projects.tasks.engine.engine)
            implementation(projects.tasks.engine.db)
        }

        webMain.dependencies {
            implementation(projects.tasks.engine.engine)
            implementation(projects.tasks.engine.db)
            implementation(projects.tasks.web)
        }
    }
}