package eu.tintera.tasks.core

import eu.tintera.tasks.Data
import eu.tintera.tasks.taskDataOf

internal operator fun Data.plus(other: Data): Data = taskDataOf(
    *(map + other.map).toList().toTypedArray()
)

internal fun Collection<Data>.sum(): Data = fold(Data.EMPTY) { acc, data ->
    acc + data
}