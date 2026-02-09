package com.emilburzo.db

import com.emilburzo.service.Flight
import com.emilburzo.service.Glider
import com.emilburzo.service.Pilot
import com.emilburzo.service.Takeoff
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import com.emilburzo.service.TIMEZONE
import org.joda.time.DateTimeZone
import java.util.*

/**
 * Created by emil on 07.12.2019.
 */
fun toLocalDateTime(date: Date): DateTime {
    return DateTime(date.time, DateTimeZone.forID(TIMEZONE))
}

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

    fun persist(flight: Flight, pilotId: Long, takeoffId: Long?, gliderId: Long) {
        transaction {
            DbFlight.insert {
                it[id] = flight.id
                it[pilot] = EntityID(pilotId, DbPilot)
                if (takeoffId != null) {
                    it[takeoff] = EntityID(takeoffId, DbTakeoff)
                }
                it[startTime] = toLocalDateTime(flight.startTime)
                it[startPoint] = flight.startPoint
                it[type] = flight.type
                it[distanceKm] = flight.distanceKm
                it[score] = flight.score
                it[airtime] = flight.airtime
                it[glider] = EntityID(gliderId, DbGlider)
                it[url] = flight.url
            }
        }
    }

    fun persist(pilot: Pilot): Long {
        return transaction {
            DbPilot.insertAndGetId {
                it[name] = pilot.name
                it[username] = pilot.username
            }.value
        }
    }

    fun persist(takeoff: Takeoff): Long {
        return transaction {
            DbTakeoff.insertAndGetId {
                it[name] = takeoff.name
                it[centroid] = takeoff.centroid
            }.value
        }
    }

    fun persist(glider: Glider): Long {
        return transaction {
            DbGlider.insertAndGetId {
                it[name] = glider.name
                it[category] = glider.category
            }.value
        }
    }

    fun findExistingFlightIds(ids: Set<Long>): Set<Long> {
        return transaction {
            DbFlight.select(DbFlight.id).where { DbFlight.id.inList(ids) }
                .map { it[DbFlight.id] }
                .toSet()
        }
    }

    fun findPilot(username: String): Pilot? {
        return transaction {
            DbPilot.selectAll().where { DbPilot.username.eq(username) }
                .limit(1)
                .map {
                    Pilot(
                        id = it[DbPilot.id].value,
                        name = it[DbPilot.name],
                        username = it[DbPilot.username]
                    )
                }.firstOrNull()
        }
    }

    fun findGlider(name: String): Glider? {
        return transaction {
            DbGlider.selectAll().where { DbGlider.name.eq(name) }
                .limit(1)
                .map {
                    Glider(
                        id = it[DbGlider.id].value,
                        name = it[DbGlider.name],
                        category = it[DbGlider.category]
                    )
                }.firstOrNull()
        }
    }

    fun findTakeoff(name: String?): Takeoff? {
        name ?: return null

        return transaction {
            DbTakeoff.selectAll().where { DbTakeoff.name.eq(name) }
                .limit(1)
                .map {
                    Takeoff(
                        id = it[DbTakeoff.id].value,
                        name = it[DbTakeoff.name],
                        centroid = it[DbTakeoff.centroid]
                    )
                }.firstOrNull()
        }
    }
}

