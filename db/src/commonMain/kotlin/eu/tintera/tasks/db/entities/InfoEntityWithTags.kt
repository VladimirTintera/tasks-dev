package eu.tintera.tasks.db.entities

import androidx.room3.Embedded
import androidx.room3.Relation

internal data class InfoEntityWithTags(
    @Embedded
    val info: InfoEntity,

    @Relation(
        parentColumn = "id", // Název sloupce id v InfoEntity (Task tabulce)
        entityColumn = "taskId" // Název sloupce taskId v TaskTag tabulce
    )
    val tags: List<TaskTag>
)