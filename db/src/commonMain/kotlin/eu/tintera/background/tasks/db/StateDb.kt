package eu.tintera.background.tasks.db

enum class StateDb {
    Enqueued,
    Blocked,
    Running,
    Cancelled,
    Succeeded,
    Failed
}