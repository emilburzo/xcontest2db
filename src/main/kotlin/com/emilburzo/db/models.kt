package com.emilburzo.db

import org.jetbrains.exposed.dao.LongIdTable
import org.jetbrains.exposed.sql.datetime

object DbFlight : LongIdTable(name = "flights") {
    val pilotName = varchar(name = "pilot_name", length = 512)
    val pilotUsername = varchar(name = "pilot_username", length = 512)
    val type = varchar(name = "type", length = 512)
    val distanceKm = double(name = "distance_km")
    val title = varchar(name = "title", length = 512).primaryKey()
    val url = varchar(name = "url", length = 512).uniqueIndex()
    val flightDate =
        datetime(name = "flight_date").index() // timezone is lost at the moment - https://github.com/JetBrains/Exposed/issues/221
}