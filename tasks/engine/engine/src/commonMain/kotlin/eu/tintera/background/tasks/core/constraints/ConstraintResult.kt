package eu.tintera.background.tasks.core.constraints

sealed interface ConstraintResult {
    data object Met : ConstraintResult       // Vše splněno (Boolean true)
    data object Unmet : ConstraintResult     // Zatím čekáme, ale šance žije (Boolean false)
    data object Failed : ConstraintResult     // Trvalé selhání (např. rodič umřel - musíme zrušit task)
}

