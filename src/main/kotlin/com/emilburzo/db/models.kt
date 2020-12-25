package com.emilburzo.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.jodatime.datetime


object DbFlight : Table(name = "flights") {
    val id = long(name = "id")
    val start = datetime(name = "start").index() // https://github.com/JetBrains/Exposed/issues/221
    val pilotName = varchar(name = "pilot_name", length = 512)
    val pilotUsername = varchar(name = "pilot_username", length = 512)
    val takeoffName = varchar(name = "takeoff_name", length = 100).nullable()
    val takeoffPoint = point(name = "takeoff_point").index()
    val type = varchar(name = "type", length = 512)
    val distanceKm = double(name = "distance_km")
    val score = double(name = "score")
    val airtime = integer(name = "airtime") // minutes
    val glider_name = varchar(name = "glider_name", length = 50)
    val glider_class = varchar(name = "glider_class", length = 50)
    val url = varchar(name = "url", length = 512).uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}