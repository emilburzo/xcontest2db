package com.emilburzo

import com.emilburzo.service.mapFlight
import com.emilburzo.service.rss.mapRssFlight
import org.junit.Test
import org.postgis.Point
import kotlin.test.assertEquals

class FlightMapperTest {

    @Test
    fun testFlightMapperRealExample0() {
        val rssFlight = mapRssFlight(
            title = "24.12.20 [1.89 km :: free_triangle] Gabriel-Alexandru Ivan",
            link = "https://www.xcontest.org/romania/zboruri/detalii:grindboyrol/24.12.2020/11:51"
        )
        val html = getResourceAsText("/flight_detail/grindboyrol.html")
        val flight = mapFlight(html = html, rssFlight = rssFlight)

        assertEquals(flight.pilot.name, "Gabriel-Alexandru Ivan")
        assertEquals(flight.pilot.username, "grindboyrol")
        assertEquals(flight.startTime.time, 1608803460000)
        assertEquals(flight.startPoint, Point(23.68482, 46.02835))
        assertEquals(flight.takeoff!!.name, "Daia Română")
        assertEquals(flight.type, "free_triangle")
        assertEquals(flight.distanceKm, 1.89)
        assertEquals(flight.score, 2.65)
        assertEquals(flight.airtime, 98)
        assertEquals(flight.glider.name, "OZONE Zeolite GT")
        assertEquals(flight.glider.category, "cat-B")
    }

}

fun getResourceAsText(path: String): String {
    return object {}.javaClass.getResource(path).readText()
}