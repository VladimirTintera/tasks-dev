package eu.tintera.background.tasks.db.entities

import androidx.room3.Embedded
import androidx.room3.Relation

data class TaskWithTagsEntity(
    @Embedded
    val info: InfoEntity,

    @Relation(
        parentColumns = ["id"],
        entityColumns = ["taskId"]
    )
    val tags: List<TaskTagEntity>
)