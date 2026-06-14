package eu.tintera.background.android

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import eu.tintera.background.android.LibraryExtension
import eu.tintera.background.common.libs
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("eu.tintera.background.kmp.library")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {

    androidLibrary {

        val pathSegments = project.path
            .split(":")
            .filter { it.isNotEmpty() }
            .toSet()

        namespace = "eu.tintera.background." + pathSegments.joinToString(".")

        compileSdk = libs.findVersion("android.compileSdk").get().requiredVersion.toInt()
        minSdk = libs.findVersion("android.minSdk").get().requiredVersion.toInt()

        withHostTest {}
    }
}

val androidLibraryExtension = extensions.getByType(KotlinMultiplatformExtension::class.java).let {
    (it as ExtensionAware).extensions.getByType(KotlinMultiplatformAndroidLibraryExtension::class.java)
}

extensions.create("androidLibrary", LibraryExtension::class.java, androidLibraryExtension)
