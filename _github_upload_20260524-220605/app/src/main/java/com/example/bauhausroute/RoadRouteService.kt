package com.example.bauhausroute

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class RoadRouteResult(
    val orderedStops: List<GeoPoint>,
    val path: List<GeoPoint>,
    val distanceMeters: Double,
    val failedSegments: Int,
    val diagnostics: List<String>
) {
    val isFallback: Boolean
        get() = failedSegments > 0

    val pathPointCount: Int
        get() = path.size
}

object RoadRouteService {
    suspend fun planRoute(points: List<GeoPoint>): RoadRouteResult = withContext(Dispatchers.IO) {
        val orderedStops = sortByNearestNeighbor(points)
        if (orderedStops.size < 2) {
            return@withContext RoadRouteResult(
                orderedStops = orderedStops,
                path = orderedStops,
                distanceMeters = 0.0,
                failedSegments = 0,
                diagnostics = listOf("ROUTE: single point, no road segment needed")
            )
        }

        val fullPath = mutableListOf<GeoPoint>()
        val diagnostics = mutableListOf<String>()
        var totalDistance = 0.0
        var failedSegments = 0

        orderedStops.zipWithNext().forEachIndexed { index, (start, end) ->
            val result = runCatching { fetchOsrmSegment(start, end) }
            val segment = result.getOrNull()
            if (segment == null) {
                failedSegments += 1
                totalDistance += start.distanceInMetersTo(end)
                appendSegment(fullPath, listOf(start, end))
                diagnostics += "SEG ${index + 1}: OSRM FAIL ${result.exceptionOrNull()?.message ?: "unknown error"}"
            } else {
                totalDistance += segment.distanceMeters
                appendSegment(fullPath, segment.path)
                diagnostics += "SEG ${index + 1}: OSRM OK ${segment.path.size} pts / ${segment.distanceMeters.toKilometerLabel()}"
            }
        }

        RoadRouteResult(
            orderedStops = orderedStops,
            path = fullPath.ifEmpty { orderedStops },
            distanceMeters = totalDistance,
            failedSegments = failedSegments,
            diagnostics = diagnostics
        )
    }

    private fun fetchOsrmSegment(start: GeoPoint, end: GeoPoint): RoadSegment {
        val coordinates = "${start.longitude},${start.latitude};${end.longitude},${end.latitude}"
        val url = URL(
            "https://router.project-osrm.org/route/v1/driving/$coordinates" +
                "?overview=full&geometries=geojson&steps=false"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "BauhausRoutePoC/1.0")
        }

        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream
                    ?.bufferedReader()
                    ?.use { reader -> reader.readText() }
                    .orEmpty()
                    .take(120)
                throw IllegalStateException("HTTP ${connection.responseCode} $errorBody")
            }

            val body = connection.inputStream.bufferedReader().use { reader -> reader.readText() }
            parseRoadSegment(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRoadSegment(body: String): RoadSegment {
        val route = JSONObject(body)
        val code = route.optString("code")
        if (code != "Ok") {
            throw IllegalStateException("OSRM code $code")
        }

        val routes = route.getJSONArray("routes")
        if (routes.length() == 0) {
            throw IllegalStateException("OSRM returned no routes")
        }

        val firstRoute = routes
            .getJSONObject(0)
        val distance = firstRoute.getDouble("distance")
        val coordinates = firstRoute
            .getJSONObject("geometry")
            .getJSONArray("coordinates")
        val path = buildList {
            for (index in 0 until coordinates.length()) {
                val coordinate = coordinates.getJSONArray(index)
                add(GeoPoint(coordinate.getDouble(1), coordinate.getDouble(0)))
            }
        }
        return RoadSegment(path = path, distanceMeters = distance)
    }

    private fun appendSegment(fullPath: MutableList<GeoPoint>, segmentPath: List<GeoPoint>) {
        if (segmentPath.isEmpty()) return
        if (fullPath.isNotEmpty() && fullPath.last() == segmentPath.first()) {
            fullPath += segmentPath.drop(1)
        } else {
            fullPath += segmentPath
        }
    }
}

private data class RoadSegment(
    val path: List<GeoPoint>,
    val distanceMeters: Double
)

fun Double.toKilometerLabel(): String {
    return String.format(Locale.US, "%.2f km", this / 1_000.0)
}
