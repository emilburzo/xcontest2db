package com.emilburzo.db

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.postgis.PGgeography
import org.postgis.Point

/**
 * Created by emil on 25.12.2020.
 */

fun Table.point(name: String): Column<Point> = registerColumn(name, PointColumnType())

private class PointColumnType() : ColumnType() {

    override fun sqlType() = "GEOGRAPHY(Point)"

    override fun valueFromDB(value: Any) = if (value is PGgeography) value.geometry else value

    override fun notNullValueToDB(value: Any): Any {
        if (value is Point) {
            return PGgeography(value)
        }
        return value
    }
}

