@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}
kotlin {
    jvmToolchain(11)

    androidTarget()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes"
        )
        optIn.addAll(
            "kotlin.uuid.ExperimentalUuidApi",
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
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(projects.db)
            implementation(projects.core)

            api(projects.api)
            api(libs.androidx.sqlite)
            api(libs.guard)
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
            implementation(libs.androidx.sqlite.web)
            implementation(
                npm("sqlite-wasm-worker",  layout.projectDirectory.dir("worker").asFile)
            )
            implementation(projects.engine)
            implementation(projects.engineDb)
        }

        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
    }
}

android {
    namespace = "eu.tintera.tasks.runtime"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()

    }

    buildTypes {
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

