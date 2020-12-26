package com.emilburzo.service

import com.emilburzo.db.Db
import com.emilburzo.service.http.Http
import com.emilburzo.service.rss.Rss
import org.slf4j.LoggerFactory
import kotlin.contracts.contract


class Xcontest2Db(
    private val db: Db,
    private val http: Http,
    private val rss: Rss
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun run() {
        // get latest from RSS feed
        val rssFlights = getRssFlights(FLIGHTS_RSS_URL)

        // fetch the ones we already know
        val existingRssFlightUrls = getExistingRssFlightUrls(rssFlights)

        // fetch info missing in the RSS feed
        val flights = rssFlights.map { mapRssFlightToFlight(it) }
            .filterNot { it.url in existingRssFlightUrls }

        // persist
        persist(flights)
    }

    private fun persist(flights: List<Flight>) {
        for (flight in flights) {
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

    private fun getExistingRssFlightUrls(rssFlights: List<RssFlight>): Set<String> {
        val rssUrls = rssFlights.map { it.url }.toSet()
        return db.findExistingUrls(rssUrls)
    }

    private fun mapRssFlightToFlight(rssFlight: RssFlight): Flight {
        val flightDetailHtml = http.getJsContent(rssFlight.url)
        return mapFlight(html = flightDetailHtml, rssFlight = rssFlight)
    }

    private fun getRssFlights(rssUrl: String): List<RssFlight> {
        val content = http.getContent(rssUrl)
        return rss.getFlights(content)
    }

}