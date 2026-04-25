package eu.tintera.tasks.db

enum class State {
    Enqueued,
    Blocked,
    Running,
    Cancelled,
    Succeeded,
    Failed
}