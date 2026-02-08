package com.emilburzo.service

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.postgis.Point
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

fun mapFlights(flightsListDoc: Document, world: Boolean): List<Flight> {
    val flightsTable = flightsListDoc.selectFirst("#flights > table > tbody") ?: return emptyList()
    return flightsTable.select("tr")
        .map { mapFlight(it, world) }
}

fun mapFlight(element: Element, world: Boolean): Flight {
    var i = 0
    val flightId = mapFlightId(element.child(i++))
    val startTime = mapStartTime(element.child(i++).child(0))
    val pilot = mapPilot(element.child(i++).child(0), world)
    val takeoff = mapTakeoff(element.child(i++).child(0))
    val type = mapFlightType(element.child(i++).child(0))
    val distanceKm = mapDistanceKm(element.child(i++).child(0))
    val score = mapScore(element.child(i++).child(0))
    val airtime = mapAirtime(element.child(i++).child(0))
    val glider = mapGlider(element.child(i++).child(0))
    i++ // skip quickview link
    val url = mapUrl(element.child(i++).child(0))

    require(flightId > 0)
    require(url.length > 20)

    return Flight(
        id = flightId,
        pilot = pilot,
        startTime = startTime,
        startPoint = takeoff.centroid,
        takeoff = takeoff,
        type = type,
        distanceKm = distanceKm,
        score = score,
        airtime = airtime,
        glider = glider,
        url = url,
    )
}

fun mapFlightId(element: Element): Long {
    return element.attr("title").split(":").last().toLong()
}

fun mapUrl(element: Element): String {
    val a = element.selectFirst(".detail")
    return a.attr("href")
}

fun mapGlider(element: Element): Glider {
    val name = element.attr("title")
    val category = element.child(0).text()

    return Glider(
        id = null,
        name = name,
        category = category
    )
}

fun mapAirtime(element: Element): Int {
    val parts = element.text().split(":")
    val hours = parts[0].trim().toInt()
    val minutes = parts[1].trim().toInt()
    return (hours * 60) + minutes
}

fun mapScore(element: Element): Double {
    return element.text().toDouble()
}

fun mapDistanceKm(element: Element): Double {
    return element.text().toDouble()
}

fun mapFlightType(element: Element): String {
    return element.attr("title")
}

fun mapTakeoff(element: Element): Takeoff {
    val a = element.child(1)
    val href = a.attr("href")
    val name = a.attr("title")
    val parts = URL(href).query
        .split("&")
        .first()
        .split("=")
        .last()
        .split(" ")
    val point = Point(parts[0].toDouble(), parts[1].toDouble())

    return Takeoff(
        id = null,
        name = name,
        centroid = point
    )
}

fun mapPilot(element: Element, world: Boolean): Pilot {
    val index = if (world) { 1 } else { 0 }
    val a = element.child(index)
    val url = a.attr("href")
    val name = a.child(0).text()
    return Pilot(
        id = null,
        name = name,
        username = url.split(":").last()
    )
}

fun mapStartTime(element: Element): Date {
    val date = element.ownText()
    val time = element.child(0).text()
    val datetime = "$date $time"
    val flightDate = LocalDateTime.parse(datetime, DateTimeFormatter.ofPattern("d.MM.yy HH:mm"))
    return Date.from(flightDate.atZone(ZoneId.of(TIMEZONE)).toInstant())
}
