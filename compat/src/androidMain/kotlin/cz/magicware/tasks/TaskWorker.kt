package cz.magicware.tasks

import android.content.Context
import androidx.work.WorkerParameters
import eu.tintera.tasks.android.TaskWorker

internal class TaskWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : TaskWorker(
    context, workerParameters
)