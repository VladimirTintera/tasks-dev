# TaskManager (KMP Work Manager)

A Kotlin Multiplatform task manager (work manager) designed for concurrent, constraint-based background work on Android, iOS, Web, and Desktop (JVM).

---

## Documentation

To help you get started with the project architecture and development workflow, please refer to the following guides:

* **[Architecture Guide](docs/architecture.md)**: Details the modular clean architecture, core concepts like `guard` (execution contexts and multiplexer tokens), repositories, database separation, and the dependency flow.
* **[Development Guide](docs/development.md)**: Explains how to build, run unit tests for individual modules, and start the demo applications on different platform targets.

---

## Project Structure

This project is highly modularized, keeping clean separation between business logic, storage, and platform-specific integrations:

* **Low-Level Execution**:
  * [guard](guard): Platform lock & token multiplexer (WakeLocks, background execution tasks).

* **Library Core & Engine**:
  * [api](tasks/api): Public library contracts, configuration interfaces, and serialization formats.
  * [core](tasks/core/core): Business logic, tasks migration, evaluation core, and repository interfaces.
  * [engine](tasks/engine/engine): Task processing, dispatching, and constraints evaluation (network, delay, parents).
  * [runtime](tasks/runtime): Library bootstrapper and lifecycle hooks.
  * [di](tasks/di): Internal dependency injection wrapper (Koin).

* **Storage & DB**:
  * [db](tasks/db): SQLite Room KMP schema & DAOs.
  * [core-db](tasks/core/db) & [engine-db](tasks/engine/db): Room DB adapter implementations for repositories.

* **Platform Modules**:
  * [android](tasks/android/android) / [android-db](tasks/android/db): Android WorkManager integration.
  * [ios](tasks/ios/ios) / [ios-db](tasks/ios/db): iOS BGTaskScheduler integrations.
  * [web](tasks/web): SQLite WebAssembly worker database driver.

* **Demo App**:
  * [shared](shared): Shared Compose Multiplatform UI logic and test handlers.
  * `androidApp`, `desktopApp`, `iosApp`, `webApp`: Specific platform target application wrappers.