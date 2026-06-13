# Development Guide

This guide describes how to set up, build, test, and run the **TaskManager** KMP project.

---

## Prerequisites

To compile and run the project, ensure your environment meets the following requirements:
* **JDK**: JDK 11 (minimum) is configured for Gradle.
* **Android SDK**: Compile SDK version 36, minimum SDK version 26.
* **iOS Development**: Xcode (required for building the iOS target).

---

## Common Build Commands

You can run these commands from the root directory using the Gradle wrapper:

### 1. Cleaning and Sestavení (Build)
- **Clean project:**
  ```bash
  ./gradlew clean
  ```
- **Full compilation & verification (without running target apps):**
  ```bash
  ./gradlew check
  ```
- **Compile Kotlin sources:**
  ```bash
  ./gradlew compileKotlin
  ```

### 2. running target applications (Demo App)

The demo apps are defined in individual platform-specific modules:

* **Desktop Application (JVM):**
  ```bash
  ./gradlew :desktopApp:run
  ```
* **Web Application (Kotlin/Wasm):** (Recommended, faster)
  ```bash
  ./gradlew :webApp:wasmJsBrowserDevelopmentRun
  ```
* **Web Application (Kotlin/JS):** (Legacy support)
  ```bash
  ./gradlew :webApp:jsBrowserDevelopmentRun
  ```
* **Android Application:**
  To build a debug APK:
  ```bash
  ./gradlew :androidApp:assembleDebug
  ```
* **iOS Application:**
  Open the `/iosApp` directory in Xcode and run the project from there.

---

## Testing

Testing is modularized. You can run all tests or focus on specific modules:

### 1. Run all unit tests
```bash
./gradlew test
```

### 2. Run tests for a specific module
* **Guard:**
  ```bash
  ./gradlew :guard:jvmTest
  ```
* **Core:**
  ```bash
  ./gradlew :core:jvmTest
  ```
* **Engine:**
  ```bash
  ./gradlew :engine:jvmTest
  ```
* **Database (`db`):**
  ```bash
  ./gradlew :db:jvmTest
  ```
* **Shared (Demo Logika):**
  ```bash
  ./gradlew :shared:jvmTest
  ```

---

## Room / KSP Code Generation

The project uses **Room for Kotlin Multiplatform** (version `3.0.0-alpha04`) and KSP for schema compilation and DAO implementation generation.
- The generated code is compiled automatically when running any build or test target.
- If you change any entity in [db](../db), compile or clean-build the project to regenerate the database bindings:
  ```bash
  ./gradlew :db:kspCommonMainKotlinMetadata
  ```
