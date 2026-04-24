package eu.tintera.tasks

import kotlin.uuid.Uuid

data class TaskInfoQuery(
    val ids: Set<Uuid>,
    val states: Set<State>,
    val tags: Set<String>,
    val uniqueNames: Set<String>
) {
    class Builder {
        private var ids = mutableSetOf<Uuid>()
        private var states = mutableSetOf<State>()
        private var tags = mutableSetOf<String>()
        private var uniqueNames = mutableSetOf<String>()

        fun addIds(ids: List<Uuid>) = apply { this.ids.addAll(ids) }
        fun addStates(states: List<State>) = apply { this.states.addAll(states) }
        fun addStates(vararg states: State) = apply { this.states.addAll(states) }
        fun addTags(tags: List<String>) = apply { this.tags.addAll(tags) }
        fun addUniqueNames(names: List<String>) = apply { this.uniqueNames.addAll(names) }

        fun build() = TaskInfoQuery(
            ids = ids,
            states = states,
            tags = tags,
            uniqueNames = uniqueNames
        )
    }

    companion object {
        fun builder() = Builder()
    }
}

