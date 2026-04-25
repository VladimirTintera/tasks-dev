package eu.tintera.tasks

import kotlin.time.Instant
import kotlin.uuid.Uuid

data class TaskInfo(
    val id: Uuid,
    val identifier: String,
    val state: State,
    val tags: Set<String>,
    val typedTags: Set<Tag>,
    val runAttemptCount: Int,
    val outputData: Any?,
    val nextScheduledTime: Instant?,
    val progress: Any?,
    val finishedAt: Instant?,
    val createdAt: Instant
)

inline fun <reified T> TaskInfo.outputAs(): T? {
    // Pro absolutní bezpečí přidáme kontrolu typu, aby to nespadlo na hloupém ClassCastException,
    // ale vyhodilo to krásnou popisnou chybu, pokud se vývojář splete v typu.
    if (outputData != null && outputData !is T) {
        error("Task '$id': Nelze přetypovat outputData typu ${outputData::class.simpleName} na ${T::class.simpleName}")
    }
    return outputData
}

inline fun <reified T> TaskInfo.progressAs(): T? {
    if (progress != null && progress !is T) {
        error("Task '$id': Nelze přetypovat progress typu ${progress::class.simpleName} na ${T::class.simpleName}")
    }
    return progress
}