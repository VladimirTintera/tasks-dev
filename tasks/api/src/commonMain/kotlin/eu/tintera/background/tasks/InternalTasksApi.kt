package eu.tintera.background.tasks

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is internal Tasks api. Do not use it in your code."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class InternalTasksApi