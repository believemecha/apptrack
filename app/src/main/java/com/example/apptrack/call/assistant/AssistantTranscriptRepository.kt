package com.example.apptrack.call.assistant

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * Persists assistant transcripts to a JSON file. No Room dependency.
 */
class AssistantTranscriptRepository(private val context: Context) {

    private val file: File by lazy {
        File(context.applicationContext.filesDir, "assistant_transcripts.json")
    }
    private val idGenerator = AtomicLong(System.currentTimeMillis())

    suspend fun insert(
        phoneNumber: String,
        transcriptText: String,
        contactName: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val id = idGenerator.incrementAndGet()
        val timestamp = System.currentTimeMillis()
        val list = loadAll().toMutableList()
        list.add(AssistantTranscript(id, phoneNumber, transcriptText, timestamp, contactName))
        saveAll(list)
        Log.d(TAG, "insert transcript for $phoneNumber: ${transcriptText.take(50)}...")
        id
    }

    fun getByPhoneNumber(phoneNumber: String): Flow<List<AssistantTranscript>> = flow {
        val list = withContext(Dispatchers.IO) {
            loadAll().filter { it.phoneNumber == phoneNumber }
                .sortedByDescending { it.timestampMillis }
        }
        emit(list)
    }

    fun getAll(): Flow<List<AssistantTranscript>> = flow {
        val list = withContext(Dispatchers.IO) {
            loadAll().sortedByDescending { it.timestampMillis }
        }
        emit(list)
    }

    private fun loadAll(): List<AssistantTranscript> {
        if (!file.exists()) return emptyList()
        return try {
            val json = file.readText()
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                AssistantTranscript(
                    id = obj.getLong("id"),
                    phoneNumber = obj.getString("phoneNumber"),
                    transcriptText = obj.getString("transcriptText"),
                    timestampMillis = obj.getLong("timestampMillis"),
                    contactName = obj.optString("contactName").takeIf { it.isNotEmpty() }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadAll failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun saveAll(list: List<AssistantTranscript>) {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("phoneNumber", t.phoneNumber)
                put("transcriptText", t.transcriptText)
                put("timestampMillis", t.timestampMillis)
                put("contactName", t.contactName ?: "")
            })
        }
        file.writeText(arr.toString())
    }

    companion object {
        private const val TAG = "AssistantTranscriptRepo"
    }
}
