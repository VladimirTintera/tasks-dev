package eu.tintera.tasks

fun Map<String, Any?>.toByteArray() = legacySerializer().encodeToBytes(taskDataOf(*this.toList().toTypedArray()))