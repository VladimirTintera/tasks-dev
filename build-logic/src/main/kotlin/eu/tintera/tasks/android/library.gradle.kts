package eu.tintera.tasks.android

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import eu.tintera.tasks.android.LibraryExtension
import eu.tintera.tasks.common.libs
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("eu.tintera.tasks.kmp.library")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {

    androidLibrary {

        val formattedProjectName = project.name.replace("-", ".")
        namespace = "eu.tintera.tasks.$formattedProjectName"

        compileSdk = libs.findVersion("android.compileSdk").get().requiredVersion.toInt()
        minSdk = libs.findVersion("android.minSdk").get().requiredVersion.toInt()
    }
}

val androidLibraryExtension = extensions.getByType(KotlinMultiplatformExtension::class.java).let {
    (it as ExtensionAware).extensions.getByType(KotlinMultiplatformAndroidLibraryExtension::class.java)
}

extensions.create("androidLibrary", LibraryExtension::class.java, androidLibraryExtension)
