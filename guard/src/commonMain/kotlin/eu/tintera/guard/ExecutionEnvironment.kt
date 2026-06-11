package eu.tintera.guard

interface ExecutionEnvironment :
    ExecutionContextProvider,
    TokenProducerRegistry,
    ExecutionContextObserverRegistry,
    TokenObservable,
    ExhaustibleObservable,
    PendingTokenObservable,
    MultiplexerObservable