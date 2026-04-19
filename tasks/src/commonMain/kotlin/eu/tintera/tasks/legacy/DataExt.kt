package eu.tintera.tasks.legacy

import kotlin.collections.plus

internal operator fun Data.plus(other: Data): Data = taskDataOf(
    *(map + other.map).toList().toTypedArray()
)

internal fun Collection<Data>.sum(): Data = fold(Data.EMPTY) { acc, data ->
    acc + data
}