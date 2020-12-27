package com.emilburzo.service

import com.emilburzo.db.Db
import com.emilburzo.service.http.Http
import org.slf4j.LoggerFactory


class Xcontest2Db(
    private val db: Db,
    private val http: Http,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun run() {
        val recentFlights = getRecentFlights(FLIGHTS_RECENT_URL)
        logger.info("found ${recentFlights.size} recent flights")

        // fetch the ones we already know
        val existingFlightUrls = getExistingFlightUrls(recentFlights)
        logger.info("found ${existingFlightUrls.size} existing urls")

        // excluding those we already know about
        val flights = recentFlights.filterNot { it.url in existingFlightUrls }
        logger.info("found ${flights.size} new flights")

        // persist
        flights.forEach {
            persist(it)
        }
        logger.info("persisted ${flights.size} flights")
    }

    private fun getRecentFlights(url: String): List<Flight> {
        val flightsListHtml = http.getJsContent(url)
        return mapFlights(flightsListHtml)
    }

    private fun getExistingFlightUrls(flights: List<Flight>): Set<String> {
        val flightUrls = flights.map { it.url }.toSet()
        return db.findExistingFlightUrls(flightUrls)
    }

    private fun persist(flight: Flight) {
        val pilotId = getOrCreate(flight.pilot)
        val takeoffId = getOrCreate(flight.takeoff)
        val gliderId = getOrCreate(flight.glider)

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