package eu.tintera.tasks.db.dao

import androidx.room3.Dao
import androidx.room3.Query
import eu.tintera.tasks.db.State
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Dao
interface CleanableTaskDao {
    @Query("SELECT id, state FROM Task WHERE state IN (:states)")
    suspend fun tasksByStates(states: List<State>): List<TaskByState>

    @Query("UPDATE Task set state = :state, finishedAt = :finishedAt, processTime = NULL, progressData = null, inputData = null  WHERE id = :id")
    suspend fun terminateTask(
        id: Uuid,
        state: State,
        finishedAt: Instant,
    )

    @Query(
        """
        UPDATE Task set 
            state = :state, 
            processTime = null,
            runAttemptCount = :runAttemptCount
        WHERE id = :id"""
    )
    suspend fun rewriteTaskState(
        id: Uuid,
        state: State,
        runAttemptCount: Int
    )
}

data class TaskByState(
    val id: Uuid,
    val state: State
)