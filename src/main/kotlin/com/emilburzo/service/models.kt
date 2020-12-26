package com.emilburzo.service

import org.postgis.Point
import java.util.*

data class Flight(
    val pilotName: String,
    val pilotUsername: String,
    val startTime: Date,
    val startPoint: Point,
    val takeoffName: String?,
    val type: String,
    val distanceKm: Double,
    val score: Double,
    val airtime: Int,
    val gliderName: String,
    val gliderClass: String,
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
