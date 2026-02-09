package com.emilburzo.db

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import net.postgis.jdbc.PGgeography
import net.postgis.jdbc.geometry.Point

/**
 * Created by emil on 25.12.2020.
 */

fun Table.point(name: String): Column<Point> = registerColumn(name, PointColumnType())

private class PointColumnType : ColumnType<Point>() {

    override fun sqlType() = "GEOGRAPHY(Point)"

    override fun valueFromDB(value: Any): Point = if (value is PGgeography) value.geometry as Point else value as Point

    override fun notNullValueToDB(value: Point): Any {
        return PGgeography(value)
    }
}

