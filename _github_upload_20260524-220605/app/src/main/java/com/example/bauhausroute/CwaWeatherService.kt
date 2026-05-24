package com.example.bauhausroute

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.Locale

data class CwaWeather(
    val locationName: String,
    val condition: String,
    val minTemperatureC: Int,
    val maxTemperatureC: Int,
    val rainChancePercent: Int
) {
    val temperatureC: Int
        get() = ((minTemperatureC + maxTemperatureC) / 2.0).toInt()
}

object CwaWeatherService {
    private const val AUTHORIZATION = "CWA-593BF25A-7E16-4438-A59A-C3D3878FE3B7"
    private const val ENDPOINT = "https://opendata.cwa.gov.tw/api/v1/rest/datastore/F-C0032-001"

    suspend fun fetchWeather(latitude: Double, longitude: Double): CwaWeather? {
        val locationName = TaiwanLocationResolver.resolve(latitude, longitude)
        return runCatching { fetchWeatherByLocation(locationName) }.getOrNull()
    }

    suspend fun fetchWeather(address: String): CwaWeather? {
        val locationName = TaiwanLocationResolver.resolve(address)
        return runCatching { fetchWeatherByLocation(locationName) }.getOrNull()
    }

    private fun fetchWeatherByLocation(locationName: String): CwaWeather? {
        val encodedLocation = URLEncoder.encode(locationName, Charsets.UTF_8.name())
        val url = URL("$ENDPOINT?Authorization=$AUTHORIZATION&format=JSON&locationName=$encodedLocation")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }

        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseWeather(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseWeather(body: String): CwaWeather? {
        val root = JSONObject(body)
        val locations = root
            .getJSONObject("records")
            .getJSONArray("location")
        if (locations.length() == 0) return null

        val location = locations.getJSONObject(0)
        val elements = location.getJSONArray("weatherElement")
        val values = mutableMapOf<String, String>()

        for (index in 0 until elements.length()) {
            val element = elements.getJSONObject(index)
            val name = element.getString("elementName")
            val firstTime = element.getJSONArray("time").getJSONObject(0)
            val parameter = firstTime.getJSONObject("parameter")
            values[name] = parameter.optString("parameterName")
        }

        return CwaWeather(
            locationName = location.getString("locationName"),
            condition = values["Wx"].orEmpty().ifBlank { "天氣資料" },
            minTemperatureC = values["MinT"]?.toIntOrNull() ?: 22,
            maxTemperatureC = values["MaxT"]?.toIntOrNull() ?: 28,
            rainChancePercent = values["PoP"]?.toIntOrNull() ?: 0
        )
    }
}

private object TaiwanLocationResolver {
    fun resolve(address: String): String {
        val normalized = address.replace("台", "臺")
        return cityNames.firstOrNull { normalized.contains(it) } ?: "臺北市"
    }

    fun resolve(latitude: Double, longitude: Double): String {
        return candidates.minBy { it.distanceScore(latitude, longitude) }.name
    }

    private val candidates = listOf(
            TaiwanCity("臺北市", 25.0375, 121.5637),
            TaiwanCity("新北市", 25.0120, 121.4657),
            TaiwanCity("桃園市", 24.9937, 121.3009),
            TaiwanCity("臺中市", 24.1477, 120.6736),
            TaiwanCity("臺南市", 22.9997, 120.2270),
            TaiwanCity("高雄市", 22.6273, 120.3014),
            TaiwanCity("基隆市", 25.1276, 121.7392),
            TaiwanCity("新竹縣", 24.8387, 121.0177),
            TaiwanCity("新竹市", 24.8138, 120.9675),
            TaiwanCity("苗栗縣", 24.5602, 120.8214),
            TaiwanCity("彰化縣", 24.0518, 120.5161),
            TaiwanCity("南投縣", 23.9609, 120.9719),
            TaiwanCity("雲林縣", 23.7092, 120.4313),
            TaiwanCity("嘉義縣", 23.4518, 120.2555),
            TaiwanCity("嘉義市", 23.4801, 120.4491),
            TaiwanCity("屏東縣", 22.5519, 120.5487),
            TaiwanCity("宜蘭縣", 24.7021, 121.7378),
            TaiwanCity("花蓮縣", 23.9872, 121.6015),
            TaiwanCity("臺東縣", 22.7972, 121.0714),
            TaiwanCity("澎湖縣", 23.5711, 119.5793),
            TaiwanCity("金門縣", 24.4368, 118.3186),
            TaiwanCity("連江縣", 26.1602, 119.9517)
        )

    private val cityNames = candidates.map { it.name }
}

private data class TaiwanCity(
    val name: String,
    val latitude: Double,
    val longitude: Double
) {
    fun distanceScore(otherLatitude: Double, otherLongitude: Double): Double {
        val latDelta = latitude - otherLatitude
        val lonDelta = longitude - otherLongitude
        return latDelta * latDelta + lonDelta * lonDelta
    }
}
