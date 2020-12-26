package com.emilburzo.service

import org.postgis.Point
import java.util.*

data class Pilot(
    val id: Long?,
    val name: String,
    val username: String,
)

data class Takeoff(
    val id: Long?,
    val name: String,
    val centroid: Point
)

data class Glider(
    val id: Long?,
    val name: String,
    val category: String,
)

data class Flight(
    val id: Long?,
    val pilot: Pilot,
    val takeoff: Takeoff?,
    val startTime: Date,
    val startPoint: Point,
    val type: String,
    val distanceKm: Double,
    val score: Double,
    val airtime: Int,
    val glider: Glider,
    val url: String,
)

// data available in the RSS feed
data class RssFlight(
    val title: String,
    val distanceKm: Double,
    val type: String,
    val pilotName: String,
    val pilotUsername: String,
    val url: String,
    val flightDate: Date
)
