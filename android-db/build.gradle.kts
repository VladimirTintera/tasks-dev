plugins {
    id("eu.tintera.tasks.android.library")
}
kotlin {

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)

            implementation(projects.android)
            implementation(projects.db)
            implementation(projects.core)
        }
    }
}