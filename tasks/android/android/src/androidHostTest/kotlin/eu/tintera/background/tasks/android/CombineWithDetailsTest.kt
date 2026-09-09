package eu.tintera.background.tasks.android

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

/**
 * The invariant under test is a regression: the pipeline used to seed its shared upstream with an
 * empty list, so it answered "nothing here" before anything had been asked. A `first()` read took
 * that answer at face value.
 */
class CombineWithDetailsTest {

    private data class Item(val id: Uuid, val label: String)

    private data class Detail(val id: Uuid, val payload: String)

    /** Pairs each item with its detail, or with `null` when no detail was found. */
    private fun Flow<List<Item>>.combined(
        detailsOf: (Set<Uuid>) -> Flow<List<Detail>>,
    ) = combineWithDetails(
        idsOf = { items -> items.map { it.id }.toSet() },
        detailsOf = detailsOf,
    ) { items, details ->
        val byId = details.associateBy { it.id }
        items.map { it.label to byId[it.id]?.payload }
    }

    @Test
    fun `while the upstream is silent - nothing is emitted`() = runTest {
        val upstream = MutableSharedFlow<List<Item>>()

        upstream.combined { flowOf(emptyList()) }.test {
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `a one-shot read waits for the upstream instead of reporting an empty result`() = runTest {
        val id = Uuid.random()
        val upstream = MutableSharedFlow<List<Item>>(replay = 1)

        // The value the upstream will produce; `first()` must not answer before it arrives.
        upstream.emit(listOf(Item(id, "task")))

        val first = upstream
            .combined { flowOf(listOf(Detail(id, "payload"))) }
            .first()

        assertEquals(listOf("task" to "payload"), first)
    }

    @Test
    fun `an empty upstream list is reported as an empty result`() = runTest {
        val upstream = MutableStateFlow(emptyList<Item>())
        var detailsQueried = false

        upstream.combined {
            detailsQueried = true
            flowOf(emptyList())
        }.test {
            assertEquals(emptyList(), awaitItem())
            cancel()
        }

        // Nothing to look up — the detail source must not be bothered.
        assertEquals(false, detailsQueried)
    }

    @Test
    fun `details are looked up for the ids the upstream carries`() = runTest {
        val first = Uuid.random()
        val second = Uuid.random()
        val upstream = MutableStateFlow(listOf(Item(first, "one")))
        val queriedIds = mutableListOf<Set<Uuid>>()

        upstream.combined { ids ->
            queriedIds += ids
            flowOf(ids.map { Detail(it, "payload-$it") })
        }.test {
            assertEquals(listOf("one" to "payload-$first"), awaitItem())

            upstream.value = listOf(Item(first, "one"), Item(second, "two"))

            // `combine` fires on every input change, so a new item shows up one emission before its
            // detail does. That transient is inherent to the shape and is asserted here on purpose:
            // consumers see the item first and the payload right after.
            assertEquals(listOf("one" to "payload-$first", "two" to null), awaitItem())
            assertEquals(
                listOf("one" to "payload-$first", "two" to "payload-$second"),
                awaitItem(),
            )
            cancel()
        }

        assertEquals(listOf(setOf(first), setOf(first, second)), queriedIds)
    }

    @Test
    fun `an unchanged set of ids does not re-query the details`() = runTest {
        val id = Uuid.random()
        val upstream = MutableStateFlow(listOf(Item(id, "before")))
        var queries = 0

        upstream.combined {
            queries++
            flowOf(listOf(Detail(id, "payload")))
        }.test {
            assertEquals(listOf("before" to "payload"), awaitItem())

            // Same id, different item — the detail lookup has no reason to run again.
            upstream.value = listOf(Item(id, "after"))
            assertEquals(listOf("after" to "payload"), awaitItem())
            cancel()
        }

        assertEquals(1, queries)
    }
}
