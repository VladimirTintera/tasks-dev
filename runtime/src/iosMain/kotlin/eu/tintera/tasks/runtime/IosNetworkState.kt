package eu.tintera.tasks.runtime

import eu.tintera.tasks.core.NetworkState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_queue_create

internal class IosNetworkState : NetworkState {

    override fun state(): Flow<NetworkState.State> = callbackFlow {
        val monitor = nw_path_monitor_create()
        val queue = dispatch_queue_create(
            "eu.tintera.tasks.network.state", null
        )

        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_set_update_handler(monitor) { path ->

            val state = nw_path_get_status(path)
            if (state == nw_path_status_satisfied)
                trySendBlocking(NetworkState.State.Connected)
            else trySendBlocking(NetworkState.State.Disconnected)
        }

        nw_path_monitor_start(monitor)

        awaitClose {
            nw_path_monitor_cancel(monitor)
        }
    }
}