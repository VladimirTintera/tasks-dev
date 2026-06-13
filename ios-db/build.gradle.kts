plugins {
    id("eu.tintera.tasks.kmp.library")
}
kotlin {

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)

            implementation(projects.engine)
            implementation(projects.db)
        }

        iosMain.dependencies {
            implementation(projects.ios)
            implementation(projects.db)
        }
    }
}