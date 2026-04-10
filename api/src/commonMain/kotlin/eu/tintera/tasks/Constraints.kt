package eu.tintera.tasks

data class Constraints(
    val requiresNetwork: Boolean = false,
    val requiresDeviceIdle: Boolean = false,
) {
    companion object {
        val NETWORK_REQUIRED = Constraints(requiresNetwork = true)
        val EMPTY = Constraints()
    }
}