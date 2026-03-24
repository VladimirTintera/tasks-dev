package eu.tintera.tasks

enum class State {
    Enqueued,
    Blocked,
    Running,
    Cancelled,
    Succeeded,
    Failed
}