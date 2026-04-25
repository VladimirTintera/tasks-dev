plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}
kotlin {

    jvmToolchain(11)

    androidTarget()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
        )

        optIn.addAll("kotlin.uuid.ExperimentalUuidApi")
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)

            implementation(projects.engine)
            implementation(projects.db)
        }
    }
}

android {
    namespace = "eu.tintera.tasks.engine.db"
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