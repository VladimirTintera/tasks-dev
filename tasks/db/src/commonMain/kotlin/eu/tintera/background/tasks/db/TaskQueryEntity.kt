package eu.tintera.background.tasks.db

import androidx.room3.RoomRawQuery
import kotlin.uuid.Uuid

internal class TaskQueryEntity(
    val ids: List<Uuid>,
    val states: List<StateDb>,
    val tags: List<String>,
    val uniqueNames: List<String>
) {

    fun toRoomRawQuery(): RoomRawQuery {
        val query = StringBuilder("SELECT t.id, t.identifier, t.runAttemptCount, t.state, t.outputData, t.processTime, t.progressData, t.finishedAt, t.createdAt, t.version, tt.taskId, tt.name FROM task t LEFT JOIN TaskTag tt ON t.id = tt.taskId WHERE 1=1")

        // Actions that bind each value to its index.
        // V KMP se binduje na objekt SQLiteStatement.
        val bindings = mutableListOf<(androidx.sqlite.SQLiteStatement, Int) -> Unit>()

        if (ids.isNotEmpty()) {
            // Vygeneruje: AND id IN (?, ?, ?)
            query.append(" AND id IN (${ids.joinToString(",") { "?" }})")

            // For every placeholder, remember how to fill it.
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
            // The KMP builder only takes the subquery:
            query.append(" AND id IN (SELECT taskId FROM TaskTag WHERE name IN (${tags.joinToString(",") { "?" }}))")

            tags.forEach { tag ->
                bindings.add { statement, index -> statement.bindText(index, tag) }
            }
        }

        if (uniqueNames.isNotEmpty()) {
            query.append(" AND uniqueName IN (${uniqueNames.joinToString(",") { "?" }})")
            uniqueNames.forEach { uniqueName ->
                bindings.add { statement, index -> statement.bindText(index, uniqueName) }
            }
        }


        return RoomRawQuery(
            sql = query.toString(),
            onBindStatement = { statement ->
                // SQLite parameter indices start at 1, not 0.
                bindings.forEachIndexed { arrayIndex, bindAction ->
                    bindAction(statement, arrayIndex + 1)
                }
            }
        )
    }
}