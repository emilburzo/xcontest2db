package com.emilburzo.service.rss

import com.emilburzo.service.Flight
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Created by emil on 24.05.2020.
 */
fun mapFlight(title: String, link: String): Flight {
    val flightMatchResult = FLIGHT_REGEX.findAll(title).first()
    val distanceKm = flightMatchResult.groupValues[2]
    val type = flightMatchResult.groupValues[3]
    val pilot = flightMatchResult.groupValues[4]

    val urlMatchResult = URL_REGEX.findAll(link).first()
    val username = urlMatchResult.groupValues[1]
    val date = urlMatchResult.groupValues[2]
    val time = urlMatchResult.groupValues[3]
    val flightDate = LocalDateTime.parse("$date $time", DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

    return Flight(
        title = title,
        distanceKm = distanceKm.toDouble(),
        type = type,
        pilotName = pilot,
        pilotUsername = username,
        url = link,
        flightDate = Date.from(flightDate.atZone(ZoneId.of("Europe/Bucharest")).toInstant())
    )
}

private val FLIGHT_REGEX =
    "([0-9]{2}\\.[0-9]{2}\\.[0-9]{2}) \\[([0-9]+\\.[0-9]+) km :: ([a-z]+_[a-z]+)\\] (.*)".toRegex()
private val URL_REGEX = ".*\\/detalii:(.*)\\/([0-9]{2}\\.[0-9]{2}\\.[0-9]{4})\\/([0-9]{2}:[0-9]{2})".toRegex()