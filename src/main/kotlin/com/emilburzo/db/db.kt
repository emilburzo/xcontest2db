package com.emilburzo.db

import com.emilburzo.service.Flight
import com.emilburzo.service.Glider
import com.emilburzo.service.Pilot
import com.emilburzo.service.Takeoff
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Created by emil on 07.12.2019.
 */
class Db(
    host: String = System.getenv("DB_HOST"),
    port: String = System.getenv("DB_PORT") ?: "5432",
    user: String = System.getenv("DB_USER") ?: "xcontest",
    pass: String = System.getenv("DB_PASS"),
    name: String = System.getenv("DB_NAME") ?: "xcontest"
) {

    init {
        Database.connect(
            url = "jdbc:postgresql://$host:$port/$name",
            driver = "org.postgresql.Driver",
            user = user,
            password = pass
        )

        // create table if it doesn't already exist
        transaction { SchemaUtils.create(DbFlight) }
    }

    fun persist(flights: List<Flight>) {
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

    fun findAllPilots(): List<Pilot> {
        return transaction {
            DbPilot.selectAll()
                .map {
                    Pilot(
                        id = it[DbPilot.id],
                        name = it[DbPilot.name],
                        username = it[DbPilot.username]
                    )
                }
        }
    }

    fun findAllGliders(): List<Glider> {
        return transaction {
            DbGlider.selectAll()
                .map {
                    Glider(
                        id = it[DbGlider.id],
                        name = it[DbGlider.name],
                        category = it[DbGlider.category]
                    )
                }
        }
    }

    fun findAllTakeoffs(): List<Takeoff> {
        return transaction {
            DbTakeoff.selectAll()
                .map {
                    Takeoff(
                        id = it[DbTakeoff.id],
                        name = it[DbTakeoff.name],
                        centroid = it[DbTakeoff.centroid]
                    )
                }
        }
    }
}

