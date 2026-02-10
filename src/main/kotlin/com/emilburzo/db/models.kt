package com.emilburzo.db

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.jodatime.datetime


object DbPilot : LongIdTable(name = "pilots") {
    val name = varchar(name = "name", length = 200)
    val username = varchar(name = "username", length = 100).uniqueIndex()
}

object DbGlider : LongIdTable(name = "gliders") {
    val name = varchar(name = "name", length = 100).uniqueIndex()
    val category = varchar(name = "category", length = 20)
}

object DbTakeoff : LongIdTable(name = "takeoffs") {
    val name = varchar(name = "name", length = 200).uniqueIndex()
    val centroid = point(name = "centroid")

    // custom indexes
    val centroidIndex = index(columns = arrayOf(centroid), indexType = "GIST")
}

object DbFlight : Table(name = "flights") {
    // columns
    val id = long(name = "id")
    val pilot = reference(name = "pilot_id", refColumn = DbPilot.id)
    val takeoff = reference(name = "takeoff_id", refColumn = DbTakeoff.id).nullable()
    val startTime = datetime(name = "start_time").index() // https://github.com/JetBrains/Exposed/issues/221
    val startPoint = point(name = "start_point")
    val type = varchar(name = "type", length = 512)
    val distanceKm = double(name = "distance_km")
    val score = double(name = "score")
    val airtime = integer(name = "airtime") // minutes
    val glider = reference(name = "glider_id", refColumn = DbGlider.id)
    val url = varchar(name = "url", length = 512)

    // custom indexes
    val startPointIndex = index(columns = arrayOf(startPoint), indexType = "GIST")

    // pk
    override val primaryKey = PrimaryKey(id)
}

object DbScrapeTask : LongIdTable(name = "scrape_tasks") {
    val url = varchar(name = "url", length = 512)
    val date = varchar(name = "date", length = 10).default("")
    val processed = bool(name = "processed").default(false)

    init {
        uniqueIndex("uq_scrape_tasks_url_date", url, date)
    }
}