package com.emilburzo.service

import com.emilburzo.db.Db
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
        val flights = getFlights(FLIGHTS_RSS_URL)
        db.persist(flights)
    }

    private fun getFlights(rssUrl: String): List<Flight> {
        val content = http.getContent(rssUrl) ?: return emptyList()
        return rss.getFlights(content)
    }

}