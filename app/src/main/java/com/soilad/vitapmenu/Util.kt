package com.soilad.vitapmenu

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@Serializable
data class FoodEntry(val name: String, val type: Int)
@Serializable
data class TimeEntry(val title: String, val start: String, val end: String, val foodEntries: List<FoodEntry>)

const val cacheSize: Long = 1 * 1024 * 1024 // 1 MB
private var client: OkHttpClient? = null

private fun getHttpClient(context: Context): OkHttpClient {
    return client ?: synchronized(Unit) {
        client ?: OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "http_cache"), cacheSize))
            .build().also { client = it }
    }
}

suspend fun getMenu(context: Context, year: String, month: String): List<List<TimeEntry>>? = withContext(Dispatchers.IO) {
    val url = "https://soilad.github.io/VITAP-Menu/menu/$year/$month/menu.json"
    val request = Request.Builder()
        .url(url)
        .build()

    try {
        val httpClient = getHttpClient(context)
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null

            val bodyString = response.body?.string() ?: return@withContext null
            Json.decodeFromString<List<List<TimeEntry>>>(bodyString)
        }
    } catch (e: Exception) {
        Log.e("Util", "Error fetching menu: ${e.message}")
        null
    }
}