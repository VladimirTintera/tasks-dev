package eu.tintera.background.tasks.core.constraints

sealed interface ConstraintResult {
    data object Met : ConstraintResult       // satisfied
    data object Unmet : ConstraintResult     // not satisfied yet, but still possible
    data object Failed : ConstraintResult    // permanently unsatisfiable (e.g. a parent died — cancel the task)
}

