package eu.tintera.tasks.db

internal enum class State {
    Enqueued,
    Blocked,
    Running,
    Cancelled,
    Succeeded,
    Failed
}