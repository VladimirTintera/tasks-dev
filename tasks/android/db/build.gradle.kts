plugins {
    id("eu.tintera.background.android.library")
}
kotlin {

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)

            implementation(projects.tasks.android.android)
            implementation(projects.tasks.db)
            implementation(projects.tasks.core.core)
        }
    }
}