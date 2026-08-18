package eu.tintera.background.tasks.db.entities

import androidx.room3.Embedded
import androidx.room3.Relation

internal data class InfoEntityWithTags(
    @Embedded
    val info: InfoEntity,

    @Relation(
        parentColumns = ["id"], // the id column of InfoEntity (the Task table)
        entityColumns = ["taskId"] // the taskId column of the TaskTag table
    )
    val tags: List<TaskTagEntity>
)