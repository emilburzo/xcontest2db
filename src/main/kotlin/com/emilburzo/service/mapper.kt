package com.emilburzo.service

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import net.postgis.jdbc.geometry.Point
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

private val log = LoggerFactory.getLogger("com.emilburzo.service.mapper")

fun mapFlights(flightsListDoc: Document, world: Boolean): List<Flight> {
    val flightsTable = flightsListDoc.selectFirst("#flights > table > tbody") ?: return emptyList()
    val rows = flightsTable.select("tr").filter { isTakeoffInRomania(it) }

    // map row by row: a single row we can't read must not cost us the other 99
    val flights = mutableListOf<Flight>()
    var firstFailure: Exception? = null
    for (row in rows) {
        try {
            flights.add(mapFlight(row, world))
        } catch (e: Exception) {
            log.error("failed to map flight row, skipping it: ${e.message} — row: ${row.text()}")
            if (firstFailure == null) firstFailure = e
        }
    }

    // ...but if every row fails, the HTML structure changed and we want to know
    if (rows.isNotEmpty() && flights.isEmpty()) {
        throw IllegalStateException(
            "all ${rows.size} flight rows failed to parse — HTML structure may have changed",
            firstFailure
        )
    }

    return flights
}

fun isTakeoffInRomania(row: Element): Boolean {
    val takeoffTd = row.child(3)
    val flag = takeoffTd.selectFirst("span.cic")
        ?: error("No flag element found in takeoff column — HTML structure may have changed. Row: ${row.text()}")
    return flag.classNames().any { it == "flag_ro" }
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

private val TAKEOFF_POINT_PATTERN = Regex("""filter\[point]=(-?[\d.]+)(?:\s|\+|%20)(-?[\d.]+)""")
private val TAKEOFF_SITE_PATTERN = Regex("""filter\[site]=([A-Za-z0-9_-]+)""")

fun mapTakeoff(element: Element): Takeoff {
    val a = element.child(1)
    val href = a.attr("href")

    return Takeoff(
        id = null,
        name = a.attr("title"),
        centroid = mapTakeoffCentroid(href),
        siteId = mapTakeoffSiteId(href),
    )
}

/**
 * Takeoffs without a registered site still link to the coordinate search
 * (".../flights-search/?filter[point]=<lon> <lat>&..."), which is the only
 * place the flights list ever exposes coordinates.
 */
fun mapTakeoffCentroid(href: String): Point? {
    val match = TAKEOFF_POINT_PATTERN.find(href) ?: return null
    val (lon, lat) = match.destructured
    return Point(lon.toDouble(), lat.toDouble())
}

/**
 * Takeoffs with a registered site link to the hot-spots page instead
 * (".../hotspots?detail#filter[date]=<date>@filter[site]=<id>.<signature>"),
 * where <id> is the base64 encoded site id and there are no coordinates at all.
 */
fun mapTakeoffSiteId(href: String): Long? {
    val encoded = TAKEOFF_SITE_PATTERN.find(href)?.groupValues?.get(1) ?: return null
    return try {
        String(Base64.getUrlDecoder().decode(encoded)).toLongOrNull()
    } catch (e: IllegalArgumentException) {
        log.warn("could not base64 decode site id '$encoded' from $href")
        null
    }
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

private val VOLUME_PATTERN = Regex("""volume\s*:\s*'(\d{4})'""")

/**
 * The season the flights list is showing, e.g. "2026" — xcontest's sites API
 * is addressed per season, and this keeps us on the same one as the page.
 */
fun mapVolume(flightsListDoc: Document): String? {
    for (script in flightsListDoc.select("script")) {
        VOLUME_PATTERN.find(script.data())?.let { return it.groupValues[1] }
    }
    return null
}

fun mapStartTime(element: Element): Date {
    val date = element.ownText()
    val time = element.child(0).text()
    val datetime = "$date $time"
    val flightDate = LocalDateTime.parse(datetime, DateTimeFormatter.ofPattern("d.MM.yy HH:mm"))
    return Date.from(flightDate.atZone(ZoneId.of(TIMEZONE)).toInstant())
}
