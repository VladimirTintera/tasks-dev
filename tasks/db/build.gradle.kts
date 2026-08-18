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

    // androidx.sqlite dělí API na nonWeb (synchronní) a web (suspend) — potřebujeme stejný šev,
    // abychom ruční migrace psali jednou pro obě větve, ne pro každý target zvlášť.
    applyDefaultHierarchyTemplate {
        common {
            group("nonWeb") {
                // withAndroidTarget() cílí na KGP `androidTarget()`; tady je android z AGP
                // pluginu com.android.kotlin.multiplatform.library, který se jmenuje "android".
                withCompilations { it.target.name == "android" }
                withIos()
                withJvm()
            }
        }
    }

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
            // Room protahuje jen typy ve svém API (SQLiteConnection v Migration). Top-level funkce
            // jako androidx.sqlite.execSQL, které používá Migration9to10, potřebují artefakt přímo.
            implementation(libs.androidx.sqlite)
            implementation(projects.tasks.core.core)

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