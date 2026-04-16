plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.serialization)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.ksp)
}
room {
    schemaDirectory("$projectDir/schemas")
    generateKotlin = true
}

kotlin {

    jvmToolchain(11)

    androidTarget()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-Xexplicit-backing-fields"
        )

        optIn.addAll("kotlin.uuid.ExperimentalUuidApi")
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.protobuf)

            api(libs.androidx.room.runtime)
            implementation(projects.core)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.kotlinx.serialization.protobuf)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
        }
        iosMain.dependencies {

        }
        jvmMain.dependencies {

        }
    }
}

android {
    namespace = "eu.tintera.tasks.db"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    with(libs.androidx.room.compiler) {
        add("kspAndroid", this)
        add("kspIosX64", this)
        add("kspIosArm64", this)
        add("kspIosSimulatorArm64", this)
        add("kspJvm", this)
    }
}