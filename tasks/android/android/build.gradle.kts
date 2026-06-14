plugins {
    id("eu.tintera.background.android.library")
}

kotlin {

    compilerOptions {
        optIn.addAll(
            "kotlin.concurrent.atomics.ExperimentalAtomicApi"
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(projects.tasks.core.core)
            implementation(projects.tasks.core.db)

            api(projects.tasks.di)
        }

        androidMain.dependencies {
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.koin.android)
        }
    }
}
