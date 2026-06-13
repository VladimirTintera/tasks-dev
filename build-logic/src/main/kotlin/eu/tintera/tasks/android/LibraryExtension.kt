package eu.tintera.tasks.android

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension

abstract class LibraryExtension(private val extension: KotlinMultiplatformAndroidLibraryExtension) {

    fun namespace(name: String) {
        extension.namespace = name
    }

    fun configure(block: KotlinMultiplatformAndroidLibraryExtension.() -> Unit) {
        extension.block()
    }
}