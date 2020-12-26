package com.emilburzo.service

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.postgis.Point
import java.net.URL

fun mapFlight(html: String, rssFlight: RssFlight): Flight {
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

fun mapAirtime(element: Element): Int {
    val parts = element.text().split(":")
    val hours = parts[0].toInt()
    val minutes = parts[1].toInt()
    return (hours * 60) + minutes
}

fun mapScore(element: Element): Double {
    return element.text().toDouble()
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