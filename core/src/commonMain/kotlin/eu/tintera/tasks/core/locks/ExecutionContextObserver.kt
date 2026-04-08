package eu.tintera.tasks.core.locks

interface ExecutionContextObserver {
    fun onStarted() { }
    // Suspend, protože běžíme stále pod systémovým zámkem a můžeme si dovolit čekat
    suspend fun onPreRelease() { }

    // Synchronní, protože systém nás zabíjí a máme jen milisekundy
    fun onPreCancel() { }
}