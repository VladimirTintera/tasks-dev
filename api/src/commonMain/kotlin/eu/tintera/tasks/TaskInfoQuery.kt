package eu.tintera.tasks

import kotlin.jvm.JvmName
import kotlin.uuid.Uuid

data class TaskInfoQuery(
    val ids: Set<Uuid>,
    val states: Set<State>,
    val tags: TagsRequest,
    val uniqueNames: Set<String>
) {
    class Builder {
        private var ids = mutableSetOf<Uuid>()
        private var states = mutableSetOf<State>()
        private var tags = mutableSetOf<String>()
        private var typedTags = mutableSetOf<Tag>()
        private var uniqueNames = mutableSetOf<String>()

        fun addIds(vararg ids: Uuid) = apply { this.ids.addAll(ids) }
        fun addIds(ids: List<Uuid>) = apply { this.ids.addAll(ids) }

        fun addStates(states: List<State>) = apply { this.states.addAll(states) }
        fun addStates(vararg states: State) = apply { this.states.addAll(states) }

        @JvmName("addRawTags")
        fun addTags(tags: List<String>) = apply { this.tags.addAll(tags) }
        fun addTags(vararg tags: String) = apply { this.tags.addAll(tags) }

        @JvmName("addTypedTags")
        fun addTags(tags: List<Tag>) = apply { this.typedTags.addAll(tags) }
        fun addTags(vararg tags: Tag) = apply { this.typedTags.addAll(tags) }

        fun addUniqueNames(uniqueNames: List<String>) = apply { this.uniqueNames.addAll(uniqueNames) }
        fun addUniqueNames(vararg uniqueName: String) = apply { this.uniqueNames.addAll(uniqueName) }

        fun build() = TaskInfoQuery(
            ids = ids,
            states = states,
            tags = TagsRequest(tags, typedTags),
            uniqueNames = uniqueNames
        )
    }

    companion object {
        fun builder() = Builder()
    }
}

