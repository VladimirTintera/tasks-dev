plugins {
    id("eu.tintera.background.kmp.library")
}
kotlin {

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        iosMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(projects.engine)
            implementation(projects.engineDb)
        }
    }
}