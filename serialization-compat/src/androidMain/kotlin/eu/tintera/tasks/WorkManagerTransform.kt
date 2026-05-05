package eu.tintera.tasks

import eu.tintera.tasks.compat.taskDataOf

fun Map<String, Any?>.toByteArray() = legacySerializer().encodeToBytes(taskDataOf(*this.toList().toTypedArray()))