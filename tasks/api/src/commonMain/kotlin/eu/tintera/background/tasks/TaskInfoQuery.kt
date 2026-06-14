package eu.tintera.background.tasks

import kotlin.jvm.JvmName
import kotlin.uuid.Uuid

data class TaskInfoQuery(
    val ids: Set<Uuid>,
    val states: Set<State>,
    val tags: Set<Tag>,
    val uniqueNames: Set<String>
)

class TaskInfoQueryBuilder {
    private val ids = mutableSetOf<Uuid>()
    private val states = mutableSetOf<State>()
    private val tags = mutableSetOf<Tag>()
    private val uniqueNames = mutableSetOf<String>()

    fun addIds(vararg ids: Uuid) = apply { this.ids.addAll(ids) }
    fun addIds(ids: Iterable<Uuid>) = apply { this.ids.addAll(ids) }

    fun addStates(states: Iterable<State>) = apply { this.states.addAll(states) }
    fun addStates(vararg states: State) = apply { this.states.addAll(states) }

    @JvmName("addRawTags")
    fun addTags(tags: Iterable<String>) = apply { this.tags.addAll(tags.map { LabelTag(it) }) }
    fun addTags(vararg tags: String) = apply { this.tags.addAll(tags.map { LabelTag(it) }) }

    @JvmName("addTypedTags")
    fun addTags(tags: Iterable<Tag>) = apply { this.tags.addAll(tags) }
    fun addTags(vararg tags: Tag) = apply { this.tags.addAll(tags) }

    fun addUniqueNames(uniqueNames: Iterable<String>) = apply { this.uniqueNames.addAll(uniqueNames) }
    fun addUniqueNames(vararg uniqueName: String) = apply { this.uniqueNames.addAll(uniqueName) }

    fun build() = TaskInfoQuery(
        ids = ids,
        states = states,
        tags = tags,
        uniqueNames = uniqueNames
    )
}

