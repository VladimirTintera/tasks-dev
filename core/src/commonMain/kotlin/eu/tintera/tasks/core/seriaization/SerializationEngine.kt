package eu.tintera.tasks.core.seriaization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

internal val frameworkJsonEngine = Json {
    // 1. Zlaté pravidlo evoluce: Pokud uživatel smaže parametr z třídy,
    // ale v DB JSONu ještě je, ignoruj ho (jinak to spadne!)
    ignoreUnknownKeys = true

    // 2. Pokud uživatel přidá do třídy nový parametr s defaultní hodnotou
    // (např. val age: Int = 18), a v DB v JSONu chybí, použije se tato defaultní hodnota.
    encodeDefaults = true

    // 3. (Volitelné) Ošetření null hodnot, pokud někdo změní non-null na nullable
    explicitNulls = false
}

internal class SerializationEngine(private val json: Json = frameworkJsonEngine) {

    fun <T> encodeToBytes(value: T, serializer: KSerializer<T>): ByteArray {
        // Převede objekt na JSON String a rovnou ho zakóduje do UTF-8 ByteArray
        return json.encodeToString(serializer, value).encodeToByteArray()
    }

    fun <T> decodeFromBytes(bytes: ByteArray, serializer: KSerializer<T>): T {
        // Vezme UTF-8 ByteArray, udělá z něj JSON String a rozparsuje ho na objekt
        return json.decodeFromString(serializer, bytes.decodeToString())
    }
}