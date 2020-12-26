package com.emilburzo.service

import com.emilburzo.db.Db
import com.emilburzo.db.DbPilot
import com.emilburzo.service.http.Http
import com.emilburzo.service.rss.Rss
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
            .filterNot { it.url in existingRssFlightUrls }

        // persist
        persist(flights)
    }

    private fun persist(flights: List<Flight>) {
        x
        db.persist(flights)
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