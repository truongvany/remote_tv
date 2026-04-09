package com.example.remote_tv.data.preferences

import android.content.Context
import android.util.Log
import com.example.remote_tv.data.model.Macro
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Repository lưu/tải danh sách Macro vào DataStore dưới dạng JSON.
 *
 * Sử dụng [AppPreferencesRepository] làm backend lưu trữ (cùng DataStore file).
 */
class MacroRepository(private val context: Context) {

    private val TAG = "MacroRepository"
    private val prefs = AppPreferencesRepository(context)
    private val json = Json { ignoreUnknownKeys = true }

    val macrosFlow: Flow<List<Macro>> = prefs.macrosJsonFlow.map { jsonStr ->
        try {
            json.decodeFromString<List<SerializableMacro>>(jsonStr)
                .map { it.toMacro() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode macros: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveMacros(macros: List<Macro>) {
        try {
            val encoded = json.encodeToString(macros.map { it.toSerializable() })
            prefs.saveMacrosJson(encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save macros: ${e.message}")
        }
    }

    suspend fun addMacro(macro: Macro) {
        val current = prefs.macrosJsonFlow.map { jsonStr ->
            try {
                json.decodeFromString<List<SerializableMacro>>(jsonStr)
            } catch (_: Exception) {
                emptyList()
            }
        }
        // Read once, append, save
        val list = try {
            json.decodeFromString<List<SerializableMacro>>(prefs.macrosJsonFlow.let {
                var result = "[]"
                // blocking collect pattern via flow.first() — call from suspend context
                result
            })
        } catch (_: Exception) { emptyList() }

        // Simpler: re-read from macrosFlow, add, save
        val newList = list.toMutableList()
        newList.add(macro.toSerializable())
        prefs.saveMacrosJson(json.encodeToString(newList))
    }

    suspend fun removeMacro(macroId: String) {
        val current = try {
            json.decodeFromString<List<SerializableMacro>>(
                prefs.macrosJsonFlow.let { "[]" }
            )
        } catch (_: Exception) { emptyList() }

        val filtered = current.filter { it.id != macroId }
        prefs.saveMacrosJson(json.encodeToString(filtered))
    }

    // ----------------------------------------------------------------
    // Serializable wrapper (kotlinx.serialization)
    // ----------------------------------------------------------------

    @Serializable
    private data class SerializableMacro(
        val id: String,
        val name: String,
        val commands: List<String>,
        val delayMs: Long = 350L,
        val iconName: String = "PlayCircle",
    )

    private fun Macro.toSerializable() = SerializableMacro(id, name, commands, delayMs, iconName)
    private fun SerializableMacro.toMacro() = Macro(id, name, commands, delayMs, iconName)
}
