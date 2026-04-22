package eu.tintera.tasks.core.preconditions

sealed interface PreconditionResult {
    data object Met : PreconditionResult       // Vše splněno (Boolean true)
    data object Unmet : PreconditionResult     // Zatím čekáme, ale šance žije (Boolean false)
    data object Failed : PreconditionResult     // Trvalé selhání (např. rodič umřel - musíme zrušit task)
    data object Cancelled : PreconditionResult
}

