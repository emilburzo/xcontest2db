package com.emilburzo

import com.emilburzo.service.mapFlightDetail
import com.emilburzo.service.mapFlights
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
        val flight = mapFlightDetail(html = html, rssFlight = rssFlight)

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

    @Test
    fun testFlightsList() {
        val html = getResourceAsText("/flights_list/recent_flights.html")
        val flights = mapFlights(html)

        assertEquals(flights.size, 100)

        val first = flights[0]
        assertEquals(first.pilot.name, "Daniel Filip")
        assertEquals(first.pilot.username, "Danielfilip")
        assertEquals(first.startTime.time, 1609074780000)
        assertEquals(first.startPoint, Point(23.545543, 46.597977))
        assertEquals(first.takeoff!!.name, "Padureni")
        assertEquals(first.type, "zbor liber")
        assertEquals(first.distanceKm, 3.94)
        assertEquals(first.score, 3.95)
        assertEquals(first.airtime, 48)
        assertEquals(first.glider.name, "OZONE Rush 5")
        assertEquals(first.glider.category, "B")

        val second = flights[1]
        assertEquals(second.pilot.name, "sergiu bal")
        assertEquals(second.pilot.username, "sergiulica_2")
        assertEquals(second.startTime.time, 1609074720000)
        assertEquals(second.startPoint, Point(23.544963, 46.59779))
        assertEquals(second.takeoff!!.name, "Padureni")
        assertEquals(second.type, "zbor liber")
        assertEquals(second.distanceKm, 4.86)
        assertEquals(second.score, 4.86)
        assertEquals(second.airtime, 58)
        assertEquals(second.glider.name, "OZONE Rush 4")
        assertEquals(second.glider.category, "B")

        val third = flights[2]
        assertEquals(third.pilot.name, "Gabriel-Alexandru Ivan")
        assertEquals(third.pilot.username, "grindboyrol")
        assertEquals(third.startTime.time, 1608810660000)
        assertEquals(third.startPoint, Point(23.68482, 46.02835))
        assertEquals(third.takeoff!!.name, "Daia Română")
        assertEquals(third.type, "triunghi plat")
        assertEquals(third.distanceKm, 1.89)
        assertEquals(third.score, 2.65)
        assertEquals(third.airtime, 98)
        assertEquals(third.glider.name, "OZONE Zeolite GT")
        assertEquals(third.glider.category, "D")
    }

}

fun getResourceAsText(path: String): String {
    return object {}.javaClass.getResource(path).readText()
}