package eu.tintera.background.guard

interface ExecutionEnvironment :
    ExecutionContextProvider,
    TokenProducerRegistry,
    ExecutionContextObserverRegistry,
    TokenObservable,
    ExhaustibleObservable,
    PendingTokenObservable,
    MultiplexerObservable