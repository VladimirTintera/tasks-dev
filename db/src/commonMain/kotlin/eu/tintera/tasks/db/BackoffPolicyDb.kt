package eu.tintera.tasks.db

import kotlinx.serialization.Serializable

@Serializable
enum class BackoffPolicyDb {
    Exponential,
    Linear
}