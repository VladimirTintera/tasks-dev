package eu.tintera.background.tasks.android

import androidx.work.WorkManager
import androidx.work.WorkQuery
import kotlinx.coroutines.flow.first
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

interface WorkManagerRepository {
    suspend fun find(ids: List<Uuid>): List<WorkInfoState>
}

internal class WorkManagerRepositoryImpl(
    private val workManager: WorkManager,
) : WorkManagerRepository {
    override suspend fun find(ids: List<Uuid>): List<WorkInfoState> {
        val workQuery = WorkQuery.Builder.fromIds(ids.map { it.toJavaUuid() }).build()
        val realWorkInfos = workManager.getWorkInfosFlow(workQuery).first()
        return realWorkInfos.map {
            WorkInfoState(
                id = it.id.toKotlinUuid(),
                state = it.state.toState(),
                runAttemptCount = it.runAttemptCount
            )
        }
    }
}