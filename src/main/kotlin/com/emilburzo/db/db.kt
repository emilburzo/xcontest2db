package com.emilburzo.db

import com.emilburzo.service.Flight
import com.emilburzo.service.RssFlight
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
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
        transaction { SchemaUtils.create(DbFlight) }

        for (flight in flights) {
            transaction {
                DbFlight.insert {
//                    it[title] = flight.title
//                    it[distanceKm] = flight.distanceKm
//                    it[type] = flight.type
//                    it[pilotName] = flight.pilotName
//                    it[pilotUsername] = flight.pilotUsername
//                    it[url] = flight.url
//                    it[start] = DateTime(flight.flightDate)
                }
            }
        }
    }

    fun findExistingUrls(rssUrls: Set<String>): Set<String> {
        return transaction {
            DbFlight.select { DbFlight.url.inList(rssUrls) }
                .map { it[DbFlight.url] }
                .toSet()
        }
    }
}

