package eu.tintera.background.tasks

import eu.tintera.background.tasks.compat.taskDataOf

/**
 * Packs the input data of a WorkManager row scheduled before the migration into the payload this
 * library stores.
 *
 * Meant to be handed to `TaskManagerConfiguration.compatTransformation`:
 * ```
 * TaskManagerConfiguration(
 *     context = context,
 *     compatTransformation = { it.toTaskDataBytes() },
 * )
 * ```
 *
 * Values `Data` cannot hold are dropped — see [taskDataOf].
 */
fun Map<String, Any?>.toTaskDataBytes(): ByteArray =
    dataSerializer().encodeToBytes(taskDataOf(*toList().toTypedArray()))
