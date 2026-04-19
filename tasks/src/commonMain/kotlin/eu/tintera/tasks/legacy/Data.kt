package eu.tintera.tasks.legacy


class Data internal constructor(
    val map: Map<String, Any>
) {
    private inline fun <reified T> getValue(
        key: String
    ): T? = map[key] as? T

    fun values() = map.values.toList()

    fun getInt(key: String): Int? = getValue(key)
    fun getString(key: String): String? = getValue(key)
    fun getLong(key: String): Long? = getValue(key)
    fun getBoolean(key: String): Boolean? = getValue(key)

    fun empty() = map.isEmpty()

    override fun toString(): String {
        return map.toString()
    }

    companion object {
        val EMPTY = Data(emptyMap())
    }
}

fun taskDataOf(vararg pairs: Pair<String, Any?>) = Data(
    pairs.mapNotNull { (key, value) ->
        value?.let {
            key to it
        }
    }.toMap()
)

