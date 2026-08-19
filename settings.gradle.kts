rootProject.name = "Background"
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

// Demo applications, only useful when this build is the one you are working on.
//
// As an included build (composite) the consumer has no use for them, and configuring them is not
// free: they drag in Compose, the JS/WASM toolchains and their own dependency graph. `gradle.parent`
// is non-null exactly when another build included this one.
if (gradle.parent == null) {
    include(":shared")
    include(":androidApp")
    include(":desktopApp")
    include(":webApp")
}

include(":guard")

include(
    ":tasks:runtime",
    ":tasks:core:core",
    ":tasks:core:db",
    ":tasks:engine:engine",
    ":tasks:engine:db",
    ":tasks:db",
    ":tasks:api",
    ":tasks:android:android",
    ":tasks:android:db",
    ":tasks:di",
    ":tasks:ios:ios",
    ":tasks:ios:db",
    ":tasks:web",
    ":tasks:compat",
    ":tasks:koin:koin",
    ":tasks:koin:json",
    ":tasks:koin:protobuf",
    ":tasks:koin:compat",
    ":tasks:serialization:json",
    ":tasks:serialization:protobuf",
    ":tasks:serialization:compat"
)

//includeBuild("external/guard") {
//    dependencySubstitution {
//        substitute(module("eu.tintera:guard")).using(project(":guard"))
//    }
//}