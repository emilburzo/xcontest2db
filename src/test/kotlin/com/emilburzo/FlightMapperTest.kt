package com.emilburzo

import com.emilburzo.db.toLocalDateTime
import com.emilburzo.service.mapFlights
import org.jsoup.Jsoup
import org.junit.Test
import net.postgis.jdbc.geometry.Point
import java.util.*
import kotlin.test.assertEquals

class FlightMapperTest {

    @Test
    fun testFlightsListWorld() {
        val document = Jsoup.parse(getResourceAsText("/flights_list/recent_flights_world.html"))
        val flights = mapFlights(document, world = true)

        assertEquals(flights.size, 100)

        val first = flights[0]
        assertEquals(4080147, first.id)
        assertEquals("Flavius Ionita", first.pilot.name)
        assertEquals("flaviusionita", first.pilot.username)
        assertEquals(1691574120000, first.startTime.time)
        assertEquals(Point(27.16835, 47.26312), first.startPoint)
        assertEquals("Spinoasa mosor", first.takeoff!!.name)
        assertEquals("FAI triangle", first.type)
        assertEquals(89.77, first.distanceKm)
        assertEquals(125.68, first.score)
        assertEquals(402, first.airtime)
        assertEquals("ADVANCE Omega XAlps 3", first.glider.name)
        assertEquals("D", first.glider.category)

        val second = flights[1]
        assertEquals(4078292, second.id)
        assertEquals("Székely István Gábor", second.pilot.name)
        assertEquals("SIG", second.pilot.username)
        assertEquals(1691660280000, second.startTime.time)
        assertEquals(Point(23.673017, 47.731383), second.startPoint)
        assertEquals("Ignis", second.takeoff!!.name)
        assertEquals("FAI triangle", second.type)
        assertEquals(6.92, second.distanceKm)
        assertEquals(11.08, second.score)
        assertEquals(49, second.airtime)
        assertEquals("ADVANCE Sigma 11", second.glider.name)
        assertEquals("C", second.glider.category)

        val third = flights[2]
        assertEquals(4077839, third.id)
        assertEquals("Alexandru Neagu", third.pilot.name)
        assertEquals("Alexandrum", third.pilot.username)
        assertEquals(1691671560000, third.startTime.time)
        assertEquals(Point(25.663973, 45.589167), third.startPoint)
        assertEquals("Bunloc", third.takeoff!!.name)
        assertEquals("free flight", third.type)
        assertEquals(3.04, third.distanceKm)
        assertEquals(3.04, third.score)
        assertEquals(7, third.airtime)
        assertEquals("OZONE Rush 4", third.glider.name)
        assertEquals("B", third.glider.category)
    }
@Test
    fun testFlightsListRomania() {
        val document = Jsoup.parse(getResourceAsText("/flights_list/recent_flights_romania.html"))
        val flights = mapFlights(document, world = false)

        assertEquals(flights.size, 100)

       val first = flights[0]
        assertEquals(2343989, first.id)
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
        assertEquals(2343991, second.id)
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
        assertEquals(2342226, third.id)
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

    /**
     * Verify that toLocalDateTime (used by db.kt to persist startTime)
     * returns Bucharest local time regardless of JVM timezone.
     *
     * The HTML shows local Bucharest time, the URL shows UTC.
     * The DB should store local time.
     */
    @Test
    fun testStartTimeDbPersistence() {
        val originalTz = TimeZone.getDefault()
        try {
            // simulate Docker (JVM in UTC)
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

            val document = Jsoup.parse(getResourceAsText("/flights_list/recent_flights_world.html"))
            val flights = mapFlights(document, world = true)

            // Flight 4080147: HTML=12:42 local (UTC+3, August), URL=09:42 (UTC)
            val first = flights[0]
            val dt1 = toLocalDateTime(first.startTime)
            assertEquals(12, dt1.hourOfDay)
            assertEquals(42, dt1.minuteOfHour)

            // Flight 4078292: HTML=12:38 local (UTC+3, August), URL=09:38 (UTC)
            val second = flights[1]
            val dt2 = toLocalDateTime(second.startTime)
            assertEquals(12, dt2.hourOfDay)
            assertEquals(38, dt2.minuteOfHour)

            // Flight 4077839: HTML=15:46 local (UTC+3, August), URL=12:46 (UTC)
            val third = flights[2]
            val dt3 = toLocalDateTime(third.startTime)
            assertEquals(15, dt3.hourOfDay)
            assertEquals(46, dt3.minuteOfHour)
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun testFlightsListWithUtc() {
        val document = Jsoup.parse(getResourceAsText("/flights_list/with_utc.html"))
        val flights = mapFlights(document, world = true)

        assertEquals(flights.size, 100)
    }

    @Test
    fun testFlightsListNpe() {
        val document = Jsoup.parse(getResourceAsText("/flights_list/npe.html"))
        val flights = mapFlights(document, world = false)

        assertEquals(flights.size, 0)
    }
}

fun getResourceAsText(path: String): String {
    return object {}.javaClass.getResource(path).readText()
}