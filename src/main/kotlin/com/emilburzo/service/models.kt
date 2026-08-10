package com.emilburzo.service

import net.postgis.jdbc.geometry.Point
import java.util.*

data class Pilot(
    val id: Long?,
    val name: String,
    val username: String,
)

/**
 * [centroid] is null when the flights list links the takeoff to the hot-spots
 * page instead of the coordinate search — those rows only carry [siteId], and
 * the coordinates have to be resolved separately.
 */
data class Takeoff(
    val id: Long?,
    val name: String,
    val centroid: Point?,
    val siteId: Long? = null,
)

data class Glider(
    val id: Long?,
    val name: String,
    val category: String,
)

data class ScrapeTask(val id: Long, val url: String, val date: String)

data class Flight(
    val id: Long,
    val pilot: Pilot,
    val takeoff: Takeoff?,
    val startTime: Date,
    val startPoint: Point?,
    val type: String,
    val distanceKm: Double,
    val score: Double,
    val airtime: Int,
    val glider: Glider,
    val url: String,
)
