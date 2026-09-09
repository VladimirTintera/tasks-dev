package eu.tintera.background.tasks.android

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.uuid.Uuid

/**
 * Combines a list with details looked up by the ids found in that list.
 *
 * The upstream is consumed once and fanned out to both branches — the list itself and the detail
 * lookup — so a single upstream subscription serves both. Details are re-queried only when the set
 * of ids actually changes, and are skipped entirely while the list is empty.
 *
 * **The upstream is shared without an invented initial value, and that is the point.** Seeding the
 * shared flow with an empty list made `combine` fire before the upstream had said anything, so the
 * first emission was always an empty result that no data source ever produced. A long-running
 * collector never noticed — the real value arrived milliseconds later — but every one-shot read
 * (`first()`) took that phantom home as the answer, and every fresh collector saw it, because the
 * sharing lives inside [channelFlow] and is therefore rebuilt per subscription. `null` marks
 * "upstream has not spoken yet" and is filtered out; an empty list downstream now means the
 * upstream really is empty.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T, D, R> Flow<List<T>>.combineWithDetails(
    idsOf: (List<T>) -> Set<Uuid>,
    detailsOf: (Set<Uuid>) -> Flow<List<D>>,
    transform: suspend (List<T>, List<D>) -> List<R>,
): Flow<List<R>> = channelFlow {
    val shared = stateIn(
        scope = this,
        started = SharingStarted.Eagerly, // safe: consumed immediately below
        initialValue = null
    )
    val items = shared.filterNotNull()

    val details = items
        .map(idsOf)
        .distinctUntilChanged()
        .flatMapLatest { ids ->
            if (ids.isEmpty()) flowOf(emptyList()) else detailsOf(ids)
        }

    combine(items, details, transform).collect { send(it) }
}
