package eu.tintera.tasks

import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val logModule = module {
    single(createdAtStart = true) {
        TokenObserver(
            scope = get(),
            observable = get()
        )
    }

    single(createdAtStart = true) {
        ExhaustibleObserver(
            scope = get(),
            observable = get()
        )
    }

    single(createdAtStart = true) {
        MultiplexerObserver(scope = get(), observable = get())
    }

    singleOf(::ExecutionContextObserver) {
        createdAtStart()
    }
}