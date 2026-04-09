package eu.tintera.guard

import android.content.Context
import android.os.PowerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// androidMain
internal class AndroidWakeLockToken(
    private val context: Context,
    isBackground: StateFlow<Boolean>,
    private val scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val timeout: Duration = 10.minutes,
    expirationHandler: () -> Unit
) : AppStateAwareToken<PowerManager.WakeLock>(isBackground, scope, dispatcher, expirationHandler) {

    // Atomická reference pro absolutní vláknovou bezpečnost
    private val expirationJob = AtomicReference<Job?>(null)

    @OptIn(ExperimentalUuidApi::class)
    override fun acquireSystemResource(): Lock<PowerManager.WakeLock> {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Guard:Tasks")

        wakeLock.acquire(timeout.inWholeMilliseconds)

        // Spustíme odpočet
        val job = scope.launch {
            delay(timeout)
            expirationHandler() // Po vypršení času zavoláme handler
        }

        // Atomicky uložíme nový job. Pokud by tam náhodou (z jakéhokoliv důvodu)
        // visel nějaký starý, rovnou ho zrušíme.
        expirationJob.getAndSet(job)?.cancel()

        return Lock(wakeLock, Uuid.random().toString())
    }

    override fun releaseSystemResource(lock: PowerManager.WakeLock) {
        // VŽDY zastavíme náš odpočet, ať je stav zámku jakýkoliv.
        // Atomicky vyjmeme referenci a nastavíme ji na null.
        expirationJob.getAndSet(null)?.cancel()

        // Následně bezpečně uvolníme systémový prostředek
        if (lock.isHeld) {
            lock.release()
        }
    }
}