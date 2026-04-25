package eu.tintera.tasks

import kotlin.reflect.KClass

interface Tag

val KClass<out Tag>.fullName: String
    get() = qualifiedName
        ?: error("Anonymous class and lambda could not be registered by type. Use registration with identifier instead.")