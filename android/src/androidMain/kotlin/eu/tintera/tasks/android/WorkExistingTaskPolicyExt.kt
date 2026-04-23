package eu.tintera.tasks.android

import androidx.work.ExistingWorkPolicy
import eu.tintera.tasks.ExistingTaskPolicy

internal fun ExistingTaskPolicy.toWorkPolicy() = when (this) {
    ExistingTaskPolicy.Keep -> ExistingWorkPolicy.KEEP
    ExistingTaskPolicy.Append -> ExistingWorkPolicy.APPEND_OR_REPLACE
    ExistingTaskPolicy.Replace -> ExistingWorkPolicy.REPLACE
}