package eu.tintera.tasks.db

import androidx.room.RoomRawQuery
import kotlin.uuid.Uuid

internal class TaskQuery private constructor(
    val ids: List<Uuid>,
    val states: List<State>,
    val tags: List<String>
) {
    companion object {
        fun builder() = Builder()
    }

    fun toRoomRawQuery(): RoomRawQuery {
        val query = StringBuilder("SELECT * FROM tasks WHERE 1=1")

        // Seznam akcí, které nabindují konkrétní hodnotu na správný index.
        // V KMP se binduje na objekt SQLiteStatement.
        val bindings = mutableListOf<(androidx.sqlite.SQLiteStatement, Int) -> Unit>()

        if (ids.isNotEmpty()) {
            // Vygeneruje: AND id IN (?, ?, ?)
            query.append(" AND id IN (${ids.joinToString(",") { "?" }})")

            // Pro každý otazník si uložíme instruci, jak ho naplnit
            ids.forEach { id ->
                bindings.add { statement, index ->
                    statement.bindText(index, id.toString())
                }
            }
        }

        if (states.isNotEmpty()) {
            query.append(" AND state IN (${states.joinToString(",") { "?" }})")
            states.forEach { state ->
                bindings.add { statement, index ->
                    statement.bindText(index, state.name)
                }
            }
        }

        if (tags.isNotEmpty()) {
            // V KMP Builderu přidáme jen subquery:
            query.append(" AND id IN (SELECT taskId FROM TaskTag WHERE name IN (${tags.joinToString(",") { "?" }}))")

            tags.forEach { tag ->
                bindings.add { statement, index -> statement.bindText(index, tag) }
            }
        }

        return RoomRawQuery(
            sql = query.toString(),
            onBindStatement = { statement ->
                // SQLite indexy parametrů začínají striktně od 1, nikoliv od 0!
                bindings.forEachIndexed { arrayIndex, bindAction ->
                    bindAction(statement, arrayIndex + 1)
                }
            }
        )
    }

    // Klasický Builder pattern pro pohodlné použití zvenčí
    class Builder {
        private var ids = emptyList<Uuid>()
        private var states = emptyList<State>()
        private var tags = emptyList<String>()

        fun fromIds(ids: List<Uuid>) = apply { this.ids = ids }
        fun fromStates(states: List<State>) = apply { this.states = states }
        fun fromTags(tags: List<String>) = apply { this.tags = tags }

        fun build() = TaskQuery(ids, states, tags)
    }
}