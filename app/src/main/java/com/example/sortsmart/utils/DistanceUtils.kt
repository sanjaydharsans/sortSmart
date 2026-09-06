package com.example.sortsmart.utils

import kotlin.math.*
object DistanceUtils {
    private const val EARTH_RADIUS_M = 6_371_000.0
    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * asin(sqrt(a))
        return (EARTH_RADIUS_M * c).toFloat()
    }
}