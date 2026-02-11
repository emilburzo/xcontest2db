package com.emilburzo

import com.emilburzo.db.toLocalDateTime
import com.emilburzo.service.extractAvailableDatesFromDoc
import com.emilburzo.service.http.parseFreeProxyList
import com.emilburzo.service.isTakeoffInRomania
import com.emilburzo.service.mapFlights
import java.net.URI
import org.jsoup.Jsoup
import org.junit.Test
import net.postgis.jdbc.geometry.Point
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    fun testNonRomanianTakeoffFiltered() {
        val html = """
            <html><body>
            <div id="flights"><table class="XClist"><tbody>
            <tr id="flight-100" class="">
                <td title="FLID:100">1</td>
                <td title="submitted: 11.02.26"><div class="full">11.02.26 <em>12:00</em></div></td>
                <td class="plt"><div class="full"><span class="cic flag_ro" title="Romania">RO</span><a class="plt" href="https://www.xcontest.org/world/en/pilots/detail:testpilot"><b>Test Pilot</b></a></div></td>
                <td><div class="full"><span class="cic flag_ro" title="Romania">RO</span><a class="lau" href="https://www.xcontest.org/world/en/flights-search/?filter[point]=25.0 45.0&amp;list[sort]=pts&amp;list[dir]=down" title="Romanian Takeoff">Romanian ...</a></div></td>
                <td><div class="disc-ff" title="free flight"><em class="hide">FF</em></div></td>
                <td class="km"><strong>10.00</strong> km</td>
                <td class="pts"><strong>10.00</strong> p.</td>
                <td class="dur"><strong><span class="d0"> </span><span class="d1">1 : 00</span></strong> h</td>
                <td class="cat-B"><div title="Test Glider" class="sponsor test"><span class="hide">B</span></div></td>
                <td><div class="for-qw"><div class="qw" title="quick view"></div></div></td>
                <td class="detail-camera"><div><a class="detail" title="flight detail" href="https://www.xcontest.org/world/en/flights/detail:testpilot/11.02.2026/10:00"><span class="hide">flight detail</span></a></div></td>
            </tr>
            <tr id="flight-101" class="odd">
                <td title="FLID:101">2</td>
                <td title="submitted: 11.02.26"><div class="full">11.02.26 <em>13:00</em></div></td>
                <td class="plt"><div class="full"><span class="cic flag_ro" title="Romania">RO</span><a class="plt" href="https://www.xcontest.org/world/en/pilots/detail:testpilot2"><b>Test Pilot 2</b></a></div></td>
                <td><div class="full"><span class="cic flag_it" title="Italy">IT</span><a class="lau" href="https://www.xcontest.org/world/en/flights-search/?filter[point]=12.0 42.0&amp;list[sort]=pts&amp;list[dir]=down" title="Italian Takeoff">Italian ...</a></div></td>
                <td><div class="disc-ff" title="free flight"><em class="hide">FF</em></div></td>
                <td class="km"><strong>5.00</strong> km</td>
                <td class="pts"><strong>5.00</strong> p.</td>
                <td class="dur"><strong><span class="d0"> </span><span class="d1">0 : 30</span></strong> h</td>
                <td class="cat-B"><div title="Test Glider 2" class="sponsor test"><span class="hide">B</span></div></td>
                <td><div class="for-qw"><div class="qw" title="quick view"></div></div></td>
                <td class="detail-camera"><div><a class="detail" title="flight detail" href="https://www.xcontest.org/world/en/flights/detail:testpilot2/11.02.2026/11:00"><span class="hide">flight detail</span></a></div></td>
            </tr>
            </tbody></table></div>
            </body></html>
        """.trimIndent()
        val doc = Jsoup.parse(html)
        val flights = mapFlights(doc, world = true)

        assertEquals(1, flights.size)
        assertEquals(100, flights[0].id)
        assertEquals("Romanian Takeoff", flights[0].takeoff!!.name)
    }

    @Test
    fun testIsTakeoffInRomania() {
        val roHtml = """<html><body><table><tbody><tr>
            <td>1</td><td>time</td><td>pilot</td>
            <td><div class="full"><span class="cic flag_ro" title="Romania">RO</span><a class="lau" href="#">Takeoff</a></div></td>
        </tr></tbody></table></body></html>""".trimIndent()
        val itHtml = """<html><body><table><tbody><tr>
            <td>1</td><td>time</td><td>pilot</td>
            <td><div class="full"><span class="cic flag_it" title="Italy">IT</span><a class="lau" href="#">Takeoff</a></div></td>
        </tr></tbody></table></body></html>""".trimIndent()

        val noFlagHtml = """<html><body><table><tbody><tr>
            <td>1</td><td>time</td><td>pilot</td>
            <td><div class="full"><a class="lau" href="#">Takeoff</a></div></td>
        </tr></tbody></table></body></html>""".trimIndent()

        val roRow = Jsoup.parse(roHtml).selectFirst("tr")!!
        val itRow = Jsoup.parse(itHtml).selectFirst("tr")!!
        val noFlagRow = Jsoup.parse(noFlagHtml).selectFirst("tr")!!

        assertEquals(true, isTakeoffInRomania(roRow))
        assertEquals(false, isTakeoffInRomania(itRow))
        assertFailsWith<IllegalStateException> { isTakeoffInRomania(noFlagRow) }
    }

    @Test
    fun testFlightsListWithUtc() {
        val document = Jsoup.parse(getResourceAsText("/flights_list/with_utc.html"))
        val flights = mapFlights(document, world = true)

        assertEquals(1, flights.size) // only 1 flight has a Romanian takeoff
    }

    @Test
    fun testFlightsListNpe() {
        val document = Jsoup.parse(getResourceAsText("/flights_list/npe.html"))
        val flights = mapFlights(document, world = false)

        assertEquals(flights.size, 0)
    }

    @Test
    fun testExtractAvailableDatesBareDates() {
        val html = """
            <html><body>
            <select>
                <option value="2025-09-30">30.09.25</option>
                <option value="2025-09-29">29.09.25</option>
                <option value="2025-09-28">28.09.25</option>
            </select>
            </body></html>
        """.trimIndent()
        val doc = Jsoup.parse(html)
        val dates = extractAvailableDatesFromDoc(doc)
        assertEquals(listOf("2025-09-30", "2025-09-29", "2025-09-28"), dates)
    }

    @Test
    fun testExtractAvailableDatesWithFlightCount() {
        val html = """
            <html><body>
            <select>
                <option value="2025-09-30">30.09.25 [10]</option>
                <option value="2025-09-29">29.09.25 [5]</option>
                <option value="2025-09-28">28.09.25 [23]</option>
            </select>
            </body></html>
        """.trimIndent()
        val doc = Jsoup.parse(html)
        val dates = extractAvailableDatesFromDoc(doc)
        assertEquals(listOf("2025-09-30", "2025-09-29", "2025-09-28"), dates)
    }

    @Test
    fun testExtractAvailableDatesSkipsNonDateSelects() {
        val html = """
            <html><body>
            <select>
                <option value="/2025/romania/zboruri/">Romania XC 2025</option>
                <option value="/2024/romania/zboruri/">Romania XC 2024</option>
            </select>
            <select>
                <option value="2025-09-30">30.09.25 [10]</option>
                <option value="2025-09-29">29.09.25</option>
            </select>
            </body></html>
        """.trimIndent()
        val doc = Jsoup.parse(html)
        val dates = extractAvailableDatesFromDoc(doc)
        assertEquals(listOf("2025-09-30", "2025-09-29"), dates)
    }

    @Test
    fun testExtractAvailableDatesEmpty() {
        val html = """
            <html><body>
            <select>
                <option value="/2025/romania/zboruri/">Romania XC 2025</option>
            </select>
            </body></html>
        """.trimIndent()
        val doc = Jsoup.parse(html)
        val dates = extractAvailableDatesFromDoc(doc)
        assertEquals(emptyList(), dates)
    }
}

class FreeProxyListTest {

    @Test
    fun testParseFreeProxyListLive() {
        val html = URI("https://free-proxy-list.net/en/").toURL().readText()
        val proxies = parseFreeProxyList(html)

        assert(proxies.isNotEmpty()) { "Expected at least one HTTPS-capable proxy, got 0. HTML structure may have changed." }
        for (proxy in proxies) {
            assert(proxy.startsWith("http://")) { "Proxy should start with http://, got: $proxy" }
            assert(proxy.matches(Regex("http://[^:]+:\\d+"))) { "Proxy should match http://host:port, got: $proxy" }
        }
    }
}

fun getResourceAsText(path: String): String {
    return object {}.javaClass.getResource(path).readText()
}