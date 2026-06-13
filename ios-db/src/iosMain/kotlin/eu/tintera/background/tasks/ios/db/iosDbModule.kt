package eu.tintera.background.tasks.ios.db

import eu.tintera.background.tasks.ios.BgTaskManagerRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val iosDbModule = module {
    factoryOf(::BgTaskManagerRepositoryImpl) bind BgTaskManagerRepository::class

}