# TaskManager (KMP Work Manager)

A Kotlin Multiplatform task manager (work manager) designed for concurrent, constraint-based background work on Android, iOS, Web, and Desktop (JVM).

---

## Documentation

To help you get started with the project architecture and development workflow, please refer to the following guides:

* **[Architecture Guide](file:///Users/vladimirtintera/Develop/tasks-dev/docs/architecture.md)**: Details the modular clean architecture, core concepts like `guard` (execution contexts and multiplexer tokens), repositories, database separation, and the dependency flow.
* **[Development Guide](file:///Users/vladimirtintera/Develop/tasks-dev/docs/development.md)**: Explains how to build, run unit tests for individual modules, and start the demo applications on different platform targets.

---

## Project Structure

This project is highly modularized, keeping clean separation between business logic, storage, and platform-specific integrations:

* **Low-Level Execution**:
  * [guard](file:///Users/vladimirtintera/Develop/tasks-dev/guard): Platform lock & token multiplexer (WakeLocks, background execution tasks).

* **Library Core & Engine**:
  * [api](file:///Users/vladimirtintera/Develop/tasks-dev/api): Public library contracts, configuration interfaces, and serialization formats.
  * [core](file:///Users/vladimirtintera/Develop/tasks-dev/core): Business logic, tasks migration, evaluation core, and repository interfaces.
  * [engine](file:///Users/vladimirtintera/Develop/tasks-dev/engine): Task processing, dispatching, and constraints evaluation (network, delay, parents).
  * [runtime](file:///Users/vladimirtintera/Develop/tasks-dev/runtime): Library bootstrapper and lifecycle hooks.
  * [di](file:///Users/vladimirtintera/Develop/tasks-dev/di): Internal dependency injection wrapper (Koin).

* **Storage & DB**:
  * [db](file:///Users/vladimirtintera/Develop/tasks-dev/db): SQLite Room KMP schema & DAOs.
  * [core-db](file:///Users/vladimirtintera/Develop/tasks-dev/core-db) & [engine-db](file:///Users/vladimirtintera/Develop/tasks-dev/engine-db): Room DB adapter implementations for repositories.

* **Platform Modules**:
  * [android](file:///Users/vladimirtintera/Develop/tasks-dev/android) / [android-db](file:///Users/vladimirtintera/Develop/tasks-dev/android-db): Android WorkManager integration.
  * [ios](file:///Users/vladimirtintera/Develop/tasks-dev/ios) / [ios-db](file:///Users/vladimirtintera/Develop/tasks-dev/ios-db): iOS BGTaskScheduler integrations.
  * [web](file:///Users/vladimirtintera/Develop/tasks-dev/web): SQLite WebAssembly worker database driver.

* **Demo App**:
  * [shared](file:///Users/vladimirtintera/Develop/tasks-dev/shared): Shared Compose Multiplatform UI logic and test handlers.
  * `androidApp`, `desktopApp`, `iosApp`, `webApp`: Specific platform target application wrappers.