package eu.tintera.background.tasks.compat

import eu.tintera.background.tasks.TaskHandler

/**
 * A handler whose input, output and progress are all the untyped [Data] map.
 *
 * This is the shape most WorkManager-based code has before it moves to typed payloads, so it makes
 * migration a matter of changing imports rather than rewriting handlers. Prefer typed
 * [TaskHandler] for anything new.
 */
typealias DataTaskHandler = TaskHandler<Data, Data, Data>
