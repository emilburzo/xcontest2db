package com.emilburzo.service

import com.emilburzo.db.Db
import com.emilburzo.service.http.Http
import com.emilburzo.service.rss.Rss
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory


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

        // persist
        db.persist(flights)
    }

    private fun getExistingRssFlightUrls(rssFlights: List<RssFlight>): Set<String> {
        val rssUrls = rssFlights.map { it.url }.toSet()
        return db.findExistingUrls(rssUrls)
    }

    private fun mapRssFlightToFlight(rssFlight: RssFlight): Flight {
        TODO("todo")
//        return Flight(
//            id =,
//            start = rssFlight.flightDate,
//            pilotName = rssFlight.pilotName,
//            pilotUsername = rssFlight.pilotUsername,
//            takeoffName =,
//            takeoffPoint =,
//            type = rssFlight.type,
//            distanceKm = rssFlight.distanceKm,
//            score =,
//            airtime =,
//            gliderName =,
//            gliderClass =,
//            url = rssFlight.url,
//        )
    }

    private fun getRssFlights(rssUrl: String): List<RssFlight> {
        val content = runBlocking {
            http.getContent(rssUrl)
        }
        return rss.getFlights(content)
    }

}