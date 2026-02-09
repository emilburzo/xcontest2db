package com.emilburzo.service

import com.emilburzo.db.Db
import com.emilburzo.service.http.Http
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory


class Xcontest2Db(
    private val db: Db,
    private val http: Http,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun fetchRecent() {
        log.info("fetching recent flights")

        processFlights(flightsListDoc = Jsoup.parse(http.getJsContent(FLIGHTS_RECENT_URL_WORLD)), world = true)
        processFlights(flightsListDoc = Jsoup.parse(http.getJsContent(FLIGHTS_RECENT_URL_ROMANIA)), world = false)
    }

    fun fetchAll() {
        log.info("fetching all flights")
        // todo env
        val baseUrls = setOf(
            "https://www.xcontest.org/2011/romania/zboruri/",
            "https://www.xcontest.org/2012/romania/zboruri/",
            "https://www.xcontest.org/2013/romania/zboruri/",
            "https://www.xcontest.org/2014/romania/zboruri/",
            "https://www.xcontest.org/2015/romania/zboruri/",
            "https://www.xcontest.org/2016/romania/zboruri/",
            "https://www.xcontest.org/2017/romania/zboruri/",
            "https://www.xcontest.org/2018/romania/zboruri/",
            "https://www.xcontest.org/2019/romania/zboruri/",
            "https://www.xcontest.org/2020/romania/zboruri/",
            "https://www.xcontest.org/2021/romania/zboruri/",
            "https://www.xcontest.org/2022/romania/zboruri/",
            "https://www.xcontest.org/2023/romania/zboruri/",
            "https://www.xcontest.org/2024/romania/zboruri/",
            "https://www.xcontest.org/2025/romania/zboruri/",
            "https://www.xcontest.org/romania/zboruri/",

            "https://www.xcontest.org/2007/world/en/flights/",
            "https://www.xcontest.org/2008/world/en/flights/",
            "https://www.xcontest.org/2009/world/en/flights/",
            "https://www.xcontest.org/2010/world/en/flights/",
            "https://www.xcontest.org/2011/world/en/flights/",
            "https://www.xcontest.org/2012/world/en/flights/",
            "https://www.xcontest.org/2013/world/en/flights/",
            "https://www.xcontest.org/2014/world/en/flights/",
            "https://www.xcontest.org/2015/world/en/flights/",
            "https://www.xcontest.org/2016/world/en/flights/",
            "https://www.xcontest.org/2017/world/en/flights/",
            "https://www.xcontest.org/2018/world/en/flights/",
            "https://www.xcontest.org/2019/world/en/flights/",
            "https://www.xcontest.org/2020/world/en/flights/",
            "https://www.xcontest.org/2021/world/en/flights/",
            "https://www.xcontest.org/2022/world/en/flights/",
            "https://www.xcontest.org/2023/world/en/flights/",
            "https://www.xcontest.org/2024/world/en/flights/",
            "https://www.xcontest.org/2025/world/en/flights/",
            "https://www.xcontest.org/world/en/flights//",
        )

        for (url in baseUrls) {
            val world = url.contains("/world/")

            // For world URLs, filter by country RO to only get Romanian flights
            val initialUrl = if (world) "$url#flights[sort]=reg@filter[country]=RO" else url
            val lastPageOffset = getLastPageOffset(Jsoup.parse(http.getJsContent(initialUrl)), world)
            require(lastPageOffset != null)
            require(lastPageOffset > 0)

            log.info("found last page offset: $lastPageOffset")

            for (offset in 0..lastPageOffset step 100) {
                val urlPaged = if (world) {
                    "$url#flights[sort]=reg@filter[country]=RO@flights[start]=$offset"
                } else {
                    "$url#flights[start]=$offset"
                }
                log.info("processing: $urlPaged")

                val flightsListDocPaged = Jsoup.parse(http.getJsContent(urlPaged))

                processFlights(flightsListDocPaged, world)

                Thread.sleep(60 * 1000) // be nice to all services involved
            }
        }
    }

    private fun getLastPageOffset(flightsListDoc: Document, world: Boolean): Int? {
        val needle = if (world) "last page" else "ultima pagină"
        val pgEdges = flightsListDoc.select(".pg-edge")
        for (pgEdge in pgEdges) {
            if (pgEdge.attr("title") == needle) {
                val href = pgEdge.attr("href")
                return href.split("=").lastOrNull()?.toInt()
            }
        }

        return null
    }

    private fun processFlights(flightsListDoc: Document, world: Boolean) {
        val flightsAll = mapFlights(flightsListDoc, world)
        log.info("parsed ${flightsAll.size} flights")

        // fetch the ones we already know
        val existingFlightIds = getExistingFlightIds(flightsAll)
        log.info("found ${existingFlightIds.size} existing flights")

        // excluding those we already know about
        val flights = flightsAll.filterNot { it.id in existingFlightIds }
        log.info("found ${flights.size} new flights")

        // persist
        flights.forEach {
            persist(it)
        }
        log.info("persisted ${flights.size} flights")
    }

    private fun getExistingFlightIds(flights: List<Flight>): Set<Long> {
        val flightIds = flights.map { it.id }.toSet()
        return db.findExistingFlightIds(flightIds)
    }

    private fun persist(flight: Flight) {
        val pilotId = getOrCreate(flight.pilot)
        val takeoffId = getOrCreate(flight.takeoff)
        val gliderId = getOrCreate(flight.glider)

        log.info("trying to persist flight: $flight, pilotId: $pilotId, takeoffId: $takeoffId, gliderId: $gliderId")

        db.persist(
            flight = flight,
            pilotId = pilotId,
            takeoffId = takeoffId,
            gliderId = gliderId,
        )
    }

    private fun getOrCreate(glider: Glider): Long {
        val db = db.findGlider(glider.name)
        if (db != null) {
            return db.id!!
        }
        return this.db.persist(glider)
    }

    private fun getOrCreate(takeoff: Takeoff?): Long? {
        takeoff ?: return null

        if (takeoff.name == "?") {
            return null
        }

        val db = db.findTakeoff(takeoff.name)
        if (db != null) {
            return db.id
        }
        return this.db.persist(takeoff)
    }

    private fun getOrCreate(pilot: Pilot): Long {
        val db = db.findPilot(pilot.username)
        if (db != null) {
            return db.id!!
        }
        return this.db.persist(pilot)
    }

}