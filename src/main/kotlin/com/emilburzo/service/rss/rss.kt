package com.emilburzo.service.rss

import com.emilburzo.service.Flight
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


/**
 * Created by emil on 07.12.2019.
 */
class Rss {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun getFlights(content: String): List<Flight> {
        val result = SyndFeedInput().build(XmlReader(content.byteInputStream()))

        return result.entries.map { mapFlight(it) }
    }

    private fun mapFlight(entry: SyndEntry): Flight {
        val flightMatchResult = FLIGHT_REGEX.findAll(entry.title).first()
        val distanceKm = flightMatchResult.groupValues[2]
        val type = flightMatchResult.groupValues[3]
        val pilot = flightMatchResult.groupValues[4]

        val urlMatchResult = URL_REGEX.findAll(entry.link).first()
        val username = urlMatchResult.groupValues[1]
        val date = urlMatchResult.groupValues[2]
        val time = urlMatchResult.groupValues[3]
        val flightDate = LocalDateTime.parse("$date $time", DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

        val x =  Flight(
            title = entry.title,
            distanceKm = distanceKm.toDouble(),
            type = type,
            pilotName = pilot,
            pilotUsername = username,
            url = entry.link,
            flightDate = Date(flightDate.toEpochSecond(ZoneOffset.of("+3")) * 1000) // FIXME
        )

        return x
    }

}

val FLIGHT_REGEX = "([0-9]{2}\\.[0-9]{2}\\.[0-9]{2}) \\[([0-9]+\\.[0-9]+) km :: ([a-z]+_[a-z]+)\\] (.*)".toRegex()
val URL_REGEX = ".*\\/detalii:(.*)\\/([0-9]{2}\\.[0-9]{2}\\.[0-9]{4})\\/([0-9]{2}:[0-9]{2})".toRegex()