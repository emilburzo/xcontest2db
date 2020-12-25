package com.emilburzo.service

import org.postgis.Point
import java.util.*

data class Flight(
    val id: Long,
    val start: Date,
    val pilotName: String,
    val pilotUsername: String,
    val takeoffName: String?,
    val takeoffPoint: Point,
    val type: String,
    val distanceKm: Double,
    val score: Double,
    val airtime: Int,
    val gliderName: String,
    val gliderClass: String,
    val url: String,
)
