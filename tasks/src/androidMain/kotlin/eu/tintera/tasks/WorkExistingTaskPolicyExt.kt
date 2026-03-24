package eu.tintera.tasks

import androidx.work.ExistingWorkPolicy

internal fun ExistingTaskPolicy.toWorkPolicy() = when (this) {
    ExistingTaskPolicy.Keep -> ExistingWorkPolicy.KEEP
    ExistingTaskPolicy.Append -> ExistingWorkPolicy.APPEND_OR_REPLACE
    ExistingTaskPolicy.Replace -> ExistingWorkPolicy.REPLACE
}