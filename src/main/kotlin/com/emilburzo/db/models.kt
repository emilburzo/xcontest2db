package com.emilburzo.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.jodatime.datetime


object DbFlight : Table(name = "flights") {
    // columns
    val id = long(name = "id")
    val start = datetime(name = "start").index() // https://github.com/JetBrains/Exposed/issues/221
    val pilotName = varchar(name = "pilot_name", length = 512)
    val pilotUsername = varchar(name = "pilot_username", length = 512)
    val takeoffName = varchar(name = "takeoff_name", length = 100).nullable()
    val takeoffPoint = point(name = "takeoff_point")
    val type = varchar(name = "type", length = 512)
    val distanceKm = double(name = "distance_km")
    val score = double(name = "score")
    val airtime = integer(name = "airtime") // minutes
    val gliderName = varchar(name = "glider_name", length = 50)
    val gliderClass = varchar(name = "glider_class", length = 50)
    val url = varchar(name = "url", length = 512).uniqueIndex()

    // custom indexes
    val takeOffPointIndex = index(columns = arrayOf(takeoffPoint), indexType = "GIST")

    // pk
    override val primaryKey = PrimaryKey(id)
}