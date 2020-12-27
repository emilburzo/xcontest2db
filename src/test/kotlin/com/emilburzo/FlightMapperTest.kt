package com.emilburzo

import com.emilburzo.service.mapFlights
import org.jsoup.Jsoup
import org.junit.Test
import org.postgis.Point
import kotlin.test.assertEquals

class FlightMapperTest {

    @Test
    fun testFlightsList() {
        val document = Jsoup.parse(getResourceAsText("/flights_list/recent_flights.html"))
        val flights = mapFlights(document)

        assertEquals(flights.size, 100)

        val first = flights[0]
        assertEquals("Daniel Filip", first.pilot.name)
        assertEquals("Danielfilip", first.pilot.username)
        assertEquals(1609074780000, first.startTime.time)
        assertEquals(Point(23.545543, 46.597977), first.startPoint)
        assertEquals("Padureni", first.takeoff!!.name)
        assertEquals("zbor liber", first.type)
        assertEquals(3.94, first.distanceKm)
        assertEquals(3.95, first.score)
        assertEquals(48, first.airtime)
        assertEquals("OZONE Rush 5", first.glider.name)
        assertEquals("B", first.glider.category)

        val second = flights[1]
        assertEquals("sergiu bal", second.pilot.name)
        assertEquals("sergiulica_2", second.pilot.username)
        assertEquals(1609074720000, second.startTime.time)
        assertEquals(Point(23.544963, 46.59779), second.startPoint)
        assertEquals("Padureni", second.takeoff!!.name)
        assertEquals("zbor liber", second.type)
        assertEquals(4.86, second.distanceKm)
        assertEquals(4.86, second.score)
        assertEquals(58, second.airtime)
        assertEquals("OZONE Rush 4", second.glider.name)
        assertEquals("B", second.glider.category)

        val third = flights[2]
        assertEquals("Gabriel-Alexandru Ivan", third.pilot.name)
        assertEquals("grindboyrol", third.pilot.username)
        assertEquals(1608810660000, third.startTime.time)
        assertEquals(Point(23.68482, 46.02835), third.startPoint)
        assertEquals("Daia Română", third.takeoff!!.name)
        assertEquals("triunghi plat", third.type)
        assertEquals(1.89, third.distanceKm)
        assertEquals(2.65, third.score)
        assertEquals(98, third.airtime)
        assertEquals("OZONE Zeolite GT", third.glider.name)
        assertEquals("D", third.glider.category)
    }

    @Test
    fun testFlightsListWithUtc() {
        val document = Jsoup.parse(getResourceAsText("/flights_list/with_utc.html"))
        val flights = mapFlights(document)

        assertEquals(flights.size, 100)
    }
}

fun getResourceAsText(path: String): String {
    return object {}.javaClass.getResource(path).readText()
}