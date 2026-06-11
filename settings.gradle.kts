rootProject.name = "TaskManager"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":shared")
include(":androidApp")
include(":desktopApp")
include(":webApp")

include(":guard")

include(":runtime")
include(":core")
include(":core-db")
include(":engine")
include(":engine-db")
include(":db")
include(":api")
include(":android")
include(":android-db")
include(":di")
include(":ios")
include(":ios-db")

include(":web")

include(":compat")

include(":koin")
include(":koin-json")
include(":koin-protobuf")
include(":koin-compat")

include(":serialization-json")
include(":serialization-protobuf")
include(":serialization-compat")

//includeBuild("external/guard") {
//    dependencySubstitution {
//        substitute(module("eu.tintera:guard")).using(project(":guard"))
//    }
//}