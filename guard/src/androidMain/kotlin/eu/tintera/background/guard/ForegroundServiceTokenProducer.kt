package eu.tintera.background.guard

import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull

@PublishedApi
internal class ForegroundServiceTokenProducer(
    private val context: Context,
    private val serviceClass: Class<out Service>,
    private val state: StateFlow<SwitchableState>
) : TokenProducer {

    override fun token(): Flow<Token> = state.mapNotNull { state ->
        if (state == SwitchableState.FOREGROUND) {
            val intent = Intent(context, serviceClass)
            try {
                ContextCompat.startForegroundService(context, intent)

                object : AbstractToken() {

                    override val tag = "ForegroundServiceToken"

                    override suspend fun onRelease() {
                        context.stopService(intent)
                    }

                    override fun onCancel() {

                    }
                }
            } catch (e: Exception) {
                // Defensive: the OS may do something unexpected, e.g. a race between our
                // StateFlow and Android's internal state.
                null
            }
        } else {
            // In the background: do not start the service and do not hand out a token.
            null
        }
    }
}

inline fun <reified T : Service> foregroundServiceTokenProducer(
    scope: CoroutineScope,
    context: Context
): TokenProducer = ForegroundServiceTokenProducer(
    context = context,
    serviceClass = T::class.java,
    state = observeAppBackgroundState(scope)
)