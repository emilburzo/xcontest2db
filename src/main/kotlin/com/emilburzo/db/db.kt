package com.emilburzo.db

import com.emilburzo.service.Flight
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.joda.time.DateTime

/**
 * Created by emil on 07.12.2019.
 */
class Db(
    private val host: String = System.getenv("DB_HOST"),
    private val port: String = System.getenv("DB_PORT") ?: "5432",
    private val user: String = System.getenv("DB_USER") ?: "xcontest",
    private val pass: String = System.getenv("DB_PASS"),
    private val name: String = System.getenv("DB_NAME") ?: "xcontest"
) {

    fun persist(flights: List<Flight>) {
        Database.connect(
            url = "jdbc:postgresql://$host:$port/$name",
            driver = "org.postgresql.Driver",
            user = user,
            password = pass
        )

        // create table if it doesn't already exist
        SchemaUtils.create(DbFlight)

        // for the urls we would attempt to insert, check if any of them already exist
        val newUrls = flights.map { it.url }.toSet()
        val existingUrls = DbFlight.select { DbFlight.url.inList(newUrls) }
            .map { it[DbFlight.url] }
            .toSet()

        for (flight in flights) {
            // skip URLs that are already in the database
            if (flight.url in existingUrls) {
                continue
            }

            DbFlight.insert {
                it[title] = flight.title
                it[distanceKm] = flight.distanceKm
                it[type] = flight.type
                it[pilotName] = flight.pilotName
                it[pilotUsername] = flight.pilotUsername
                it[url] = flight.url
                it[flightDate] = DateTime(flight.flightDate)
            }
        }
    }
}

