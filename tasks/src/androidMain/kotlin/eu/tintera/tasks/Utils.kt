package eu.tintera.tasks

import androidx.work.Data

internal fun Data.toData() = taskDataOf(
    *keyValueMap.mapNotNull { (key, value) ->
        key.takeIf { it != TaskWorker.TASK_IDENTIFIER }?.let {
            key to value
        }
    }.toTypedArray()
)