package eu.tintera.tasks.db.entities

import androidx.room3.Embedded
import androidx.room3.Relation

data class TaskWithTags(
    @Embedded
    val info: InfoEntity,

    @Relation(
        parentColumn = "id",     // ID z Task tabulky (obsažené v InfoEntity)
        entityColumn = "taskId"  // Cizí klíč v tabulce TaskTag
    )
    val tags: List<TaskTag>
)