package com.example.humsafar.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Open-Meteo free weather API (no key required).
 * Returns current weather for a location.
 */
object WeatherService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    data class WeatherResult(
        val tempC: Double,
        val weatherCode: Int,
        val description: String
    )

    suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherResult? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,weather_code"
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val current = json.getJSONObject("current")
            val temp = current.getDouble("temperature_2m")
            val code = current.getInt("weather_code")
            WeatherResult(temp, code, weatherCodeDescription(code))
        } catch (e: Exception) {
            null
        }
    }

    private fun weatherCodeDescription(code: Int): String = when {
        code == 0 -> "Clear sky"
        code in 1..3 -> "Partly cloudy"
        code in 45..48 -> "Foggy"
        code in 51..67 -> "Rain"
        code in 71..77 -> "Snow"
        code in 80..82 -> "Rain showers"
        code in 85..86 -> "Snow showers"
        code in 95..99 -> "Thunderstorm"
        else -> "Cloudy"
    }

    /** Suggestions: umbrella (rain), jacket (cold/snow), hat (sunny) */
    fun weatherSuggestions(tempC: Double, weatherCode: Int): List<String> {
        val suggestions = mutableListOf<String>()
        if (weatherCode in 51..67 || weatherCode in 80..82 || weatherCode in 95..99) suggestions.add("Bring umbrella")
        if (tempC < 18 || weatherCode in 71..77) suggestions.add("Bring jacket")
        if (weatherCode in 0..2 && tempC > 20) suggestions.add("Bring hat")
        return suggestions.ifEmpty { listOf("Enjoy your visit!") }
    }
}
