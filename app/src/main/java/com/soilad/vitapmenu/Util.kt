package com.soilad.vitapmenu

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.time.YearMonth

const val cacheSize: Long = 1 * 1024 * 1024 // 1 MB
private var client: OkHttpClient? = null

private fun getHttpClient(context: Context): OkHttpClient {
    return client ?: synchronized(Unit) {
        client ?: OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "http_cache"), cacheSize))
            .build().also { client = it }
    }
}

suspend fun getMenu(context: Context, date: YearMonth, forceRefresh: Boolean = false): List<List<TimeEntry>>? = withContext(Dispatchers.IO) {
    val url = "https://soilad.github.io/VITAP-Menu/menu/${date.year}/${date.monthValue.toString().padStart(2, '0')}/menu.json"
    val request = Request.Builder()
        .url(url)
        .apply { if (forceRefresh) cacheControl(CacheControl.FORCE_NETWORK) }
        .build()

    try {
        val httpClient = getHttpClient(context)
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null

            val bodyString = response.body.string()
            Json.decodeFromString<List<List<TimeEntry>>>(bodyString)
        }
    } catch (e: Exception) {
        Log.e("Util", "Error fetching menu: ${e.message}")
        null
    }
}