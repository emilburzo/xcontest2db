package com.emilburzo.service

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.postgis.Point
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

fun mapFlights(html: String): List<Flight> {
    val flightsList = Jsoup.parse(html)
    val flights = flightsList.selectFirst("#flights > table > tbody")
        .select("tr")
        .map { mapFlight(it) }

    return flights
}

fun mapFlight(element: Element): Flight {
    var i = 1
    val startTime = mapStartTime(element.child(i++).child(0))
    val pilot = mapPilot(element.child(i++).child(0))
    val takeoff = mapTakeoff(element.child(i++).child(0))
    val type = mapFlightType(element.child(i++).child(0))
    val distanceKm = mapDistanceKm(element.child(i++).child(0))
    val score = mapScore(element.child(i++).child(0))
    val airtime = mapAirtime(element.child(i++).child(0))
    val glider = mapGlider(element.child(i++).child(0))
    i++ // quickview link
    val url = mapUrl(element.child(i++).child(0))

    return Flight(
        id = null,
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

fun mapUrl(element: Element): String {
    val a = element.child(0)
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

fun mapPilot(element: Element): Pilot {
    val a = element.child(0)
    val url = a.attr("href")
    val name = a.child(0).text()
    return Pilot(
        id = null,
        name = name,
        username = url.split(":").last()
    )
}

fun mapStartTime(element: Element): Date {
    val datetime = element.text().split("=")[0]
    val flightDate = LocalDateTime.parse(datetime, DateTimeFormatter.ofPattern("d.MM.yy HH:mm"))
    return Date.from(flightDate.atZone(ZoneId.of("Europe/Bucharest")).toInstant()) // todo env
}

fun mapFlightDetail(html: String, rssFlight: RssFlight): Flight {
    val flightDetail = Jsoup.parse(html)
    val takeoffName = mapTakeoffName(flightDetail.selectFirst(SELECTOR_TAKEOFF))
    val startPoint = mapStartPoint(flightDetail.selectFirst(SELECTOR_TAKEOFF))
    val score = mapScore(flightDetail.selectFirst(SELECTOR_SCORE))
    val airtime = mapAirtime(flightDetail.selectFirst(SELECTOR_AIRTIME))
    val gliderName = mapGliderName(flightDetail.selectFirst(SELECTOR_GLIDER))
    val gliderCategory = mapGliderCategory(flightDetail.selectFirst(SELECTOR_GLIDER))

    return Flight(
        id = null,
        pilot = Pilot(
            id = null,
            name = rssFlight.pilotName,
            username = rssFlight.pilotUsername
        ),
        startTime = rssFlight.flightDate,
        startPoint = startPoint,
        takeoff = Takeoff(
            id = null,
            name = takeoffName,
            centroid = startPoint
        ),
        type = rssFlight.type,
        distanceKm = rssFlight.distanceKm,
        score = score,
        airtime = airtime,
        glider = Glider(
            id = null,
            name = gliderName,
            category = gliderCategory
        ),
        url = rssFlight.url,
    )
}

fun mapGliderCategory(element: Element): String {
    return element.attr("class")
        .split(" ")
        .last()
}

fun mapGliderName(element: Element): String {
    return element.attr("title")
}

private fun mapStartPoint(element: Element): Point {
    // https://www.xcontest.org/world/en/flights-search/?filter[point]=23.68482 46.02835&list[sort]=pts
    val parts = URL(element.attr("href")).query
        .split("&")
        .first()
        .split("=")
        .last()
        .split(" ")
    return Point(parts[0].toDouble(), parts[1].toDouble())
}

private fun mapTakeoffName(element: Element): String {
    // "Celosvětové vyhledávání přeletů:  Daia Română"
    return element.attr("title")
        .split(":")
        .last()
        .trim()
}


const val SELECTOR_TAKEOFF =
    "#flight > div.XCflight > div.XCbaseInfo > table > tbody > tr:nth-child(3) > td > a:nth-child(2)"

const val SELECTOR_SCORE =
    "#flight > div.XCflight > div.XCbaseInfo > table > tbody > tr:nth-child(4) > td.pts.nowrap > strong"

const val SELECTOR_AIRTIME = "#flight > div.XCflight > div.XCbaseInfo > table > tbody > tr:nth-child(6) > td.dur > span"

const val SELECTOR_GLIDER = "#flight > div.XCflight > div.XCbaseInfo > table > tbody > tr:nth-child(5) > td"