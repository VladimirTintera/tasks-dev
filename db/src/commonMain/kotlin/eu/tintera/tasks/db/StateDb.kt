package eu.tintera.tasks.db

enum class StateDb {
    Enqueued,
    Blocked,
    Running,
    Cancelled,
    Succeeded,
    Failed
}