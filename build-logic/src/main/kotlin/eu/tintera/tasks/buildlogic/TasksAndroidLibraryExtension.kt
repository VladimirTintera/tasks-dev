package eu.tintera.tasks.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension

abstract class TasksAndroidLibraryExtension(private val extension: KotlinMultiplatformAndroidLibraryExtension) {

    fun namespace(name: String) {
        extension.namespace = name
    }

    fun configure(block: KotlinMultiplatformAndroidLibraryExtension.() -> Unit) {
        extension.block()
    }
}