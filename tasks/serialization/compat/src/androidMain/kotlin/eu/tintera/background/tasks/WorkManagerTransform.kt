package eu.tintera.background.tasks

import eu.tintera.background.tasks.compat.taskDataOf

fun Map<String, Any?>.toByteArray() = legacySerializer().encodeToBytes(taskDataOf(*this.toList().toTypedArray()))