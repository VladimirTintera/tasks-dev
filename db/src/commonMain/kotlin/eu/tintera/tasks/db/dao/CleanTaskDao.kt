package eu.tintera.tasks.db.dao

import androidx.room3.Dao
import androidx.room3.Query
import eu.tintera.tasks.db.StateDb
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Dao
interface CleanableTaskDao {
    @Query("SELECT id, state FROM Task WHERE state IN (:states)")
    suspend fun tasksByStates(states: List<StateDb>): List<TaskByState>

    @Query("UPDATE Task set state = :state, finishedAt = :finishedAt, processTime = NULL, progressData = null, inputData = null  WHERE id = :id")
    suspend fun terminateTask(
        id: Uuid,
        state: StateDb,
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
        state: StateDb,
        runAttemptCount: Int
    )
}

data class TaskByState(
    val id: Uuid,
    val state: StateDb
)