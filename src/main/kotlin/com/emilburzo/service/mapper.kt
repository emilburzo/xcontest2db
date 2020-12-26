package com.emilburzo.service

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.postgis.Point

fun mapFlight(html: String, rssFlight: RssFlight): Flight {
    val flightDetail = Jsoup.parse(html)
    val takeoffName = mapTakeoffName(flightDetail.selectFirst(SELECTOR_TAKEOFF_NAME))
    val startPoint = mapStartPoint(flightDetail.selectFirst(SELECTOR_TAKEOFF_NAME))
    val score = mapScore(flightDetail.selectFirst(SELECTOR_TAKEOFF_NAME))
    val airtime = mapAirtime(flightDetail.selectFirst(SELECTOR_TAKEOFF_NAME))
    val gliderName = mapGliderName(flightDetail.selectFirst(SELECTOR_TAKEOFF_NAME))
    val gliderClass = mapGliderClass(flightDetail.selectFirst(SELECTOR_TAKEOFF_NAME))

    return Flight(
        pilotName = rssFlight.pilotName,
        pilotUsername = rssFlight.pilotUsername,
        startTime = rssFlight.flightDate,
        startPoint = startPoint,
        takeoffName = takeoffName,
        type = rssFlight.type,
        distanceKm = rssFlight.distanceKm,
        score = score,
        airtime = airtime,
        gliderName = gliderName,
        gliderClass = gliderClass,
        url = rssFlight.url,
    )
}

fun mapGliderClass(element: Element?): String {
    TODO("Not yet implemented")
}

fun mapGliderName(element: Element?): String {
    TODO("Not yet implemented")
}

fun mapAirtime(element: Element?): Int {
    TODO("Not yet implemented")
}

fun mapScore(element: Element?): Double {
    TODO("Not yet implemented")
}

private fun mapStartPoint(element: Element?): Point {
    element ?: throw IllegalArgumentException()

    val href = element.attr("href")
    val title = element.attr("title")
    TODO("Not yet implemented")
}

private fun mapTakeoffName(element: Element?): String {
    element ?: throw IllegalArgumentException()

    // "Celosvětové vyhledávání přeletů:  Daia Română"
    return element.attr("title")
        .split(":")
        .last()
        .trim()
}


const val SELECTOR_TAKEOFF_NAME =
    "#flight > div.XCflight > div.XCbaseInfo > table > tbody > tr:nth-child(3) > td > a:nth-child(2)"