import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import eu.tintera.tasks.buildlogic.TasksAndroidLibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {

    jvmToolchain(11)

    androidLibrary {

        val formattedProjectName = project.name.replace("-", ".")
        namespace = "eu.tintera.tasks.android.$formattedProjectName"

        compileSdk = libs.findVersion("android.compileSdk").get().requiredVersion.toInt()
        minSdk = libs.findVersion("android.minSdk").get().requiredVersion.toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

val androidLibraryExtension = extensions.getByType(KotlinMultiplatformExtension::class.java).let {
    (it as ExtensionAware).extensions.getByType(KotlinMultiplatformAndroidLibraryExtension::class.java)
}

extensions.create("androidLibrary", TasksAndroidLibraryExtension::class.java, androidLibraryExtension)