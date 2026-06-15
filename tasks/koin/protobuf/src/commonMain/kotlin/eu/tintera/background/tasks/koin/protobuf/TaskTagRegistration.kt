package eu.tintera.background.tasks.koin.protobuf

import eu.tintera.background.tasks.Tag
import eu.tintera.background.tasks.koin.taskTag
import eu.tintera.background.tasks.protobufTagSerializer
import org.koin.core.module.Module


inline fun <reified T : Tag> Module.taskTag(
    identifier: String
) = taskTag<T>(
    identifier = identifier,
    serializer = protobufTagSerializer<T>()
)