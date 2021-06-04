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

        val flightsListDoc = Jsoup.parse(http.getJsContent(FLIGHTS_RECENT_URL))
        processFlights(flightsListDoc)
    }

    fun fetchAll() {
        log.info("fetching all flights")
        // todo env
        val baseUrls = setOf(
            "https://www.xcontest.org/romania/zboruri/",
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
        )

        for (url in baseUrls) {
            val lastPageOffset = getLastPageOffset(Jsoup.parse(http.getJsContent(url)))
            require(lastPageOffset != null)
            require(lastPageOffset > 0)

            log.info("found last page offset: $lastPageOffset")

            for (offset in 0..lastPageOffset step 100) {
                val urlPaged = "$url#flights[start]=$offset"
                log.info("processing: $urlPaged")

                val flightsListDocPaged = Jsoup.parse(http.getJsContent(urlPaged))

                processFlights(flightsListDocPaged)

                Thread.sleep(60 * 1000) // be nice to all services involved
            }
        }
    }

    private fun getLastPageOffset(flightsListDoc: Document): Int? {
        val pgEdges = flightsListDoc.select(".pg-edge")
        for (pgEdge in pgEdges) {
            if (pgEdge.attr("title") == "ultima pagină") {
                val href = pgEdge.attr("href")
                return href.split("=").lastOrNull()?.toInt()
            }
        }

        return null
    }

    private fun processFlights(flightsListDoc: Document) {
        val flightsAll = mapFlights(flightsListDoc)
        log.info("parsed ${flightsAll.size} flights")

        // fetch the ones we already know
        val existingFlightUrls = getExistingFlightUrls(flightsAll)
        log.info("found ${existingFlightUrls.size} existing urls")

        // excluding those we already know about
        val flights = flightsAll.filterNot { it.url in existingFlightUrls }
        log.info("found ${flights.size} new flights")

        // persist
        flights.forEach {
            persist(it)
        }
        log.info("persisted ${flights.size} flights")
    }

    private fun getExistingFlightUrls(flights: List<Flight>): Set<String> {
        val flightUrls = flights.map { it.url }.toSet()
        return db.findExistingFlightUrls(flightUrls)
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