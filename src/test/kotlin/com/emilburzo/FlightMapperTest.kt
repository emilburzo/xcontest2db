package com.emilburzo

import com.emilburzo.service.rss.mapFlight
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Created by emil on 24.05.2020.
 */
class FlightMapperTest {

    @Test
    fun testFlightMapperRealExample0() {
        val flight = mapFlight(
            title = "16.05.20 [1.81 km :: fai_triangle] Ciprian Cucerzan",
            link = "https://www.xcontest.org/romania/zboruri/detalii:cuci/16.05.2020/11:09"
        )

        assertEquals(flight.distanceKm, 1.81)
        assertEquals(flight.type, "fai_triangle")
        assertEquals(flight.pilotName, "Ciprian Cucerzan")
        assertEquals(flight.pilotUsername, "cuci")
        assertEquals(flight.flightDate.time, 1589616540000)
    }

    @Test
    fun testFlightMapperRealExample1() {
        val flight = mapFlight(
            title = "23.05.20 [2.60 km :: free_triangle] Emese Fodor",
            link = "https://www.xcontest.org/romania/zboruri/detalii:Emese/23.05.2020/14:26"
        )

        assertEquals(flight.distanceKm, 2.60)
        assertEquals(flight.type, "free_triangle")
        assertEquals(flight.pilotName, "Emese Fodor")
        assertEquals(flight.pilotUsername, "Emese")
        assertEquals(flight.flightDate.time, 1590233160000)
    }

    @Test
    fun testFlightMapperRealExample2() {
        val flight = mapFlight(
            title = "23.05.20 [1.67 km :: fai_triangle] sergiu bal",
            link = "https://www.xcontest.org/romania/zboruri/detalii:sergiulica_2/23.05.2020/14:26"
        )

        assertEquals(flight.distanceKm, 1.67)
        assertEquals(flight.type, "fai_triangle")
        assertEquals(flight.pilotName, "sergiu bal")
        assertEquals(flight.pilotUsername, "sergiulica_2")
        assertEquals(flight.flightDate.time, 1590233160000)
    }

    @Test
    fun testFlightMapperRealExample3() {
        val flight = mapFlight(
            title = "23.05.20 [49.34 km :: free_flight] DAN PIRGHIE",
            link = "https://www.xcontest.org/romania/zboruri/detalii:DANPIRGHIE/23.05.2020/12:33"
        )

        assertEquals(flight.distanceKm, 49.34)
        assertEquals(flight.type, "free_flight")
        assertEquals(flight.pilotName, "DAN PIRGHIE")
        assertEquals(flight.pilotUsername, "DANPIRGHIE")
        assertEquals(flight.flightDate.time, 1590226380000)
    }

    @Test
    fun testFlightMapperDaylightSavings() {
        val flight = mapFlight(
            title = "23.02.20 [49.34 km :: free_flight] DAN PIRGHIE",
            link = "https://www.xcontest.org/romania/zboruri/detalii:DANPIRGHIE/23.02.2020/12:33"
        )

        assertEquals(flight.distanceKm, 49.34)
        assertEquals(flight.type, "free_flight")
        assertEquals(flight.pilotName, "DAN PIRGHIE")
        assertEquals(flight.pilotUsername, "DANPIRGHIE")
        assertEquals(flight.flightDate.time, 1582453980000)
    }

    @Test
    fun testFlightMapperNewMonth() {
        val flight = mapFlight(
            title = "01.07.20 [20.58 km :: free_triangle] sergiu bal",
            link = "https://www.xcontest.org/romania/zboruri/detalii:sergiulica_2/1.07.2020/10:50"
        )

        assertEquals(flight.distanceKm, 20.58)
        assertEquals(flight.type, "free_triangle")
        assertEquals(flight.pilotName, "sergiu bal")
        assertEquals(flight.pilotUsername, "sergiulica_2")
        assertEquals(flight.flightDate.time, 1593589800000)
    }

}