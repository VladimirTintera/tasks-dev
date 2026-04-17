package eu.tintera.tasks

import kotlin.uuid.Uuid

interface TaskScope<Input, Progress> {
    val taskId: Uuid
    val data: Input
    val retryCount: Int

    val parents: List<ParentData>

    suspend fun setForegroundInfo(foregroundInfo: ForegroundInfo): Boolean
    suspend fun setProgress(data: Progress)
}

// 1. ZÁKLADNÍ FUNKCE (Vrací seznam všech výstupů daného typu)
inline fun <reified T> TaskScope<*, *>.parentOutputs(
    identifier: String
): List<T> = parents
    .filter { it.identifier == identifier }
    .map {
        it.data as? T ?: error(
            "🚨 Typový nesoulad u rodičovského úkolu '$identifier'! " +
                    "Očekáván typ '${T::class.simpleName}', ale přijat '${it.data?.let { d -> d::class.simpleName } ?: "null"}'."
        )
    }

// 2. STRIKTNÍ ZÍSKÁNÍ JEDNOHO RODIČE (Fail-Fast)
inline fun <reified T> TaskScope<*, *>.parentOutput(
    identifier: String
): T = parentOutputs<T>(identifier).firstOrNull()
    ?: error("🚨 Rodičovský úkol '$identifier' nebyl nalezen. Ujisti se, že je správně napojen v grafu!")

// 3. BENEVOLENTNÍ ZÍSKÁNÍ VOLITELNÉHO RODIČE (Novinka pro flexibilitu)
inline fun <reified T> TaskScope<*, *>.parentOutputOrNull(
    identifier: String
): T? = parents.filter {
    it.identifier == identifier
}.firstNotNullOfOrNull { it.data as? T }


// --- ALIAS EXTENZE PRO TYPOVÉ TŘÍDY ---
// Poznámka: Používáme out Any, aby to sežralo jakýkoliv TaskHandler
inline fun <reified T, reified R : TaskHandler<out Any, out Any, out Any>> TaskScope<*, *>.parentOutputs(): List<T> =
    parentOutputs(R::class.fullName)

inline fun <reified T, reified R : TaskHandler<out Any, out Any, out Any>> TaskScope<*, *>.parentOutput(): T =
    parentOutput(R::class.fullName)

inline fun <reified T, reified R : TaskHandler<out Any, out Any, out Any>> TaskScope<*, *>.parentOutputOrNull(): T? =
    parentOutputOrNull(R::class.fullName)

// 1. Získá VŠECHNY výstupy daného typu od všech rodičů
inline fun <reified T : Any> TaskScope<*, *>.parentOutputsOfType(): List<T> =
    parents.mapNotNull { it.data as? T }

// 2. Striktní získání (Fail-Fast), pokud nutně potřebuješ právě jeden výstup tohoto typu
inline fun <reified T : Any> TaskScope<*, *>.parentOutputOfType(): T =
    parentOutputsOfType<T>().firstOrNull()
        ?: error("🚨 V grafu nebyl nalezen žádný rodičovský úkol vracející typ '${T::class.simpleName}'!")

// 3. Benevolentní získání
inline fun <reified T : Any> TaskScope<*, *>.parentOutputOfTypeOrNull(): T? =
    parentOutputsOfType<T>().firstOrNull()

typealias LegacyTaskScope = TaskScope<Data, Data>