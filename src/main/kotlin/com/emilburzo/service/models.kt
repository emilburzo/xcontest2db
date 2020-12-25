package com.emilburzo.service

import java.util.*

data class RssFlight(
    val title: String,
    val distanceKm: Double,
    val type: String,
    val pilotName: String,
    val pilotUsername: String,
    val url: String,
    val flightDate: Date
)
