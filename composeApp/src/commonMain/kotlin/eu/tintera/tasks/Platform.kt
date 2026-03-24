package eu.tintera.tasks

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform