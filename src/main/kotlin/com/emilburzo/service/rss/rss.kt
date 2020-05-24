package com.emilburzo.service.rss

import com.emilburzo.service.Flight
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.slf4j.LoggerFactory


/**
 * Created by emil on 07.12.2019.
 */
class Rss {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun getFlights(content: String): List<Flight> {
        val result = SyndFeedInput().build(XmlReader(content.byteInputStream()))
        logger.info("found ${result.entries.size} flights")
        return result.entries.map { mapFlight(it.title, it.link) }
    }

}

