package com.example.bauhausroute

import org.osmdroid.util.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

fun sortByNearestNeighbor(points: List<GeoPoint>): List<GeoPoint> {
    if (points.size <= 2) return points

    val ordered = mutableListOf(points.first())
    val remaining = points.drop(1).toMutableList()

    while (remaining.isNotEmpty()) {
        val current = ordered.last()
        val next = remaining.minBy { current.distanceInMetersTo(it) }
        ordered += next
        remaining -= next
    }

    return ordered
}

fun GeoPoint.distanceInMetersTo(other: GeoPoint): Double {
    val earthRadiusMeters = 6_371_000.0
    val lat1 = Math.toRadians(latitude)
    val lat2 = Math.toRadians(other.latitude)
    val deltaLat = Math.toRadians(other.latitude - latitude)
    val deltaLon = Math.toRadians(other.longitude - longitude)
    val haversine = sin(deltaLat / 2).pow(2) +
        cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2)
    return earthRadiusMeters * 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
}

