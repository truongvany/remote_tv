package com.example.remote_tv.data.preferences

import android.content.Context
import android.util.Log
import com.example.remote_tv.data.model.Macro
import kotlinx.coroutines.flow.first
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
        val current = loadSerializableMacros().toMutableList()
        current.removeAll { it.id == macro.id }
        current.add(macro.toSerializable())
        prefs.saveMacrosJson(json.encodeToString(current))
    }

    suspend fun updateMacro(macro: Macro) {
        addMacro(macro)
    }

    suspend fun removeMacro(macroId: String) {
        val current = loadSerializableMacros()

        val filtered = current.filter { it.id != macroId }
        prefs.saveMacrosJson(json.encodeToString(filtered))
    }

    private suspend fun loadSerializableMacros(): List<SerializableMacro> {
        val jsonStr = prefs.macrosJsonFlow.first()
        return try {
            json.decodeFromString(jsonStr)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode macros on load: ${e.message}")
            emptyList()
        }
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
