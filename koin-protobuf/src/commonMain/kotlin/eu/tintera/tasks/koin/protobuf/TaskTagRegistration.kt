package eu.tintera.tasks.koin.protobuf

import eu.tintera.tasks.Tag
import eu.tintera.tasks.fullName
import eu.tintera.tasks.koin.taskTag
import eu.tintera.tasks.protobufTagSerializer
import org.koin.core.module.Module


inline fun <reified T : Tag> Module.taskTag(
    identifier: String = T::class.fullName
) = taskTag<T>(
    identifier = identifier,
    serializer = protobufTagSerializer<T>()
)