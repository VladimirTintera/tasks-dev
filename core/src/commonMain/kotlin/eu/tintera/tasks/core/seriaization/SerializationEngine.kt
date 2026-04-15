package eu.tintera.tasks.core.seriaization

import eu.tintera.tasks.Data
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf

class SerializationEngine(
    private val json: Json = Json {
        // 1. Zlaté pravidlo evoluce: Pokud uživatel smaže parametr z třídy,
        // ale v DB JSONu ještě je, ignoruj ho (jinak to spadne!)
        ignoreUnknownKeys = true

        // 2. Pokud uživatel přidá do třídy nový parametr s defaultní hodnotou
        // (např. val age: Int = 18), a v DB v JSONu chybí, použije se tato defaultní hodnota.
        encodeDefaults = true

        // 3. (Volitelné) Ošetření null hodnot, pokud někdo změní non-null na nullable
        explicitNulls = false
    },
    // Přidáme nativní Protobuf engine z kotlinx
    private val protoBuf: ProtoBuf = ProtoBuf { encodeDefaults = true }
) {

    @Suppress("UNCHECKED_CAST")
    fun <T> encodeToBytes(value: T, serializer: KSerializer<T>): ByteArray {
        // VÝHYBKA PRO LEGACY DATA
        // Zkontrolujeme, zda je to starý typ (porovnáme reference objektů)
        if (serializer === DataSerializer) {
            val legacyData = value as Data

            // 1. Tady použiješ svou starou logiku na přemapování!
            val surrogate = legacyData.toSerializableTaskData() // Tvá stará metoda

            // 2. Zakódujeme to rovnou jako reálný Protobuf ByteArray!
            return protoBuf.encodeToByteArray(SerializableTaskData.serializer(), surrogate)
        }

        // NOVÝ SVĚT (Pro všechny nové typové objekty)
        // Převede na JSON String a zakóduje do UTF-8 ByteArray
        return json.encodeToString(serializer, value).encodeToByteArray()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> decodeFromBytes(bytes: ByteArray, serializer: KSerializer<T>): T {
        // VÝHYBKA PRO LEGACY DATA
        if (serializer === DataSerializer) {
            // 1. Oživíme Protobuf bajty z databáze do tvého starého DTO
            val surrogate = protoBuf.decodeFromByteArray(SerializableTaskData.serializer(), bytes)

            // 2. Přemapujeme to zpět do tvé staré třídy Data
            return surrogate.toData() as T // Tvá stará metoda
        }

        // NOVÝ SVĚT
        // Rozparsuje UTF-8 JSON z databáze
        return json.decodeFromString(serializer, bytes.decodeToString())
    }
}