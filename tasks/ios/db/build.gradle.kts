plugins {
    id("eu.tintera.background.kmp.library")
}
kotlin {

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)

            implementation(projects.tasks.engine.engine)
            implementation(projects.tasks.db)
        }

        iosMain.dependencies {
            implementation(projects.tasks.ios.ios)
            implementation(projects.tasks.db)
        }
    }
}