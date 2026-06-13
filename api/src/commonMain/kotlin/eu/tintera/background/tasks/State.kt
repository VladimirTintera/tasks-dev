package eu.tintera.background.tasks

enum class State {
    Enqueued,
    Blocked,
    Running,
    Cancelled,
    Succeeded,
    Failed
}