package eu.tintera.background.tasks.db

import kotlinx.serialization.Serializable

@Serializable
enum class BackoffPolicyDb {
    Exponential,
    Linear
}