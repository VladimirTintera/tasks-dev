package eu.tintera.guard

import android.content.Context
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class WakeLockTokenProducer(
    private val context: Context,
    private val timeout: Duration = 10.minutes,
    private val scope: CoroutineScope
) : TokenProducer {
    override fun token(): Flow<Token> = flow {
        emit(
            WakeLockToken(
                context = context,
                timeout = timeout,
                scope = scope
            )
        )
    }
}

internal class WakeLockToken(
    context: Context,
    private val timeout: Duration = 10.minutes,
    scope: CoroutineScope
) : AbstractToken() {

    @OptIn(ExperimentalUuidApi::class)
    override val tag: String = "WakeLockToken:${Uuid.random()}"

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Guard:Tasks")

    init {
        wakeLock.acquire(timeout.inWholeMilliseconds)
    }

    val expirationJob = scope.launch {
        delay(timeout)
        finishWithCancel()
    }

    override suspend fun onRelease() {
        finish()
    }

    override fun onCancel() {
        finish()
    }

    private fun finish() {
        expirationJob.cancel()

        if (wakeLock.isHeld) {
            try {
                wakeLock.release()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}