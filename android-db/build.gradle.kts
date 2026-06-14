plugins {
    id("eu.tintera.background.android.library")
}
kotlin {

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)

            implementation(projects.android)
            implementation(projects.db)
            implementation(projects.core)
        }
    }
}