package com.emilburzo.service

import com.emilburzo.db.Db
import com.emilburzo.service.http.Http
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import kotlin.jvm.javaClass
import kotlin.random.Random

private val log = LoggerFactory.getLogger(Xcontest2Db::class.java)

private val BASE_URLS = setOf(
    "https://www.xcontest.org/world/en/flights/",
)

class Xcontest2Db(
    private val db: Db,
    private val http: Http,
) {

    fun fetchRecent() {
        log.info("fetching recent flights")

        val useProxy = System.getenv("USE_FREE_PROXY")?.lowercase() == "true"

        if (useProxy) {
            val proxies = http.fetchFreeProxies()
            if (proxies.isEmpty()) {
                log.error("no free proxies available, cannot fetch recent flights")
                return
            }

            fetchRecentWithProxies(proxies)
        } else {
            processFlights(flightsListDoc = Jsoup.parse(http.getJsContent(FLIGHTS_RECENT_URL_WORLD)), world = true)
            processFlights(flightsListDoc = Jsoup.parse(http.getJsContent(FLIGHTS_RECENT_URL_ROMANIA)), world = false)
        }
    }

    private fun fetchRecentWithProxies(proxies: List<String>) {
        for (proxy in proxies) {
            log.info("trying proxy: $proxy")
            try {
                val worldHtml = http.getJsContent(FLIGHTS_RECENT_URL_WORLD, proxy = proxy)
                val worldDoc = Jsoup.parse(worldHtml)
                val worldFlights = mapFlights(worldDoc, world = true)

                if (worldFlights.isEmpty()) {
                    log.warn("proxy $proxy returned 0 flights for world, trying next")
                    continue
                }
                log.info("proxy $proxy works — got ${worldFlights.size} world flights")

                val romaniaHtml = http.getJsContent(FLIGHTS_RECENT_URL_ROMANIA, proxy = proxy)
                val romaniaDoc = Jsoup.parse(romaniaHtml)
                val romaniaFlights = mapFlights(romaniaDoc, world = false)

                log.info("proxy $proxy — got ${romaniaFlights.size} romania flights")

                processFlights(worldDoc, world = true)
                processFlights(romaniaDoc, world = false)
                return
            } catch (e: Exception) {
                log.warn("proxy $proxy failed: ${e.message}")
            }
        }

        log.error("all ${proxies.size} proxies failed, could not fetch recent flights")
    }

    fun populate() {
        log.info("populating scrape tasks")

        for (url in BASE_URLS) {
            val world = url.contains("/world/")

            val initialUrl = if (world) "$url#flights[sort]=reg@filter[country]=RO" else url
            val overviewDoc = Jsoup.parse(http.getJsContent(initialUrl))
            val overviewLastPageOffset = getLastPageOffset(overviewDoc, world) ?: 0

            log.info("$url: overview last page offset = $overviewLastPageOffset")

            if (overviewLastPageOffset < 900) {
                log.info("$url: under 1000-item cap, creating single task")
                db.insertScrapeTask(url, "")
            } else {
                log.info("$url: hit 1000-item cap, splitting by date")
                val dates = extractAvailableDatesWithRetry(initialUrl, overviewDoc)

                for (date in dates) {
                    db.insertScrapeTask(url, date)
                }
            }

            delay()
        }

        log.info("populate complete")
    }

    fun scrape() {
        val tasks = db.findUnprocessedScrapeTasks()
        log.info("found ${tasks.size} unprocessed scrape tasks")

        for (task in tasks.shuffled()) {
            val world = task.url.contains("/world/")
            log.info("processing task #${task.id}: url=${task.url}, date=${task.date}")

            try {
                if (task.date.isEmpty()) {
                    scrapeWithoutDateFilter(task.url, world)
                } else {
                    scrapeWithDateFilter(task.url, task.date, world)
                }

                db.markScrapeTaskProcessed(task.id)
                log.info("marked task #${task.id} as processed")
            } catch (e: Exception) {
                log.error("failed to process task #${task.id}: ${e.message}")
            }

            delay()
        }

        log.info("scrape complete")
    }

    private fun scrapeWithoutDateFilter(url: String, world: Boolean) {
        val initialUrl = if (world) "$url#flights[sort]=reg@filter[country]=RO" else url
        val overviewDoc = Jsoup.parse(http.getJsContent(initialUrl))
        val lastPageOffset = getLastPageOffset(overviewDoc, world) ?: 0

        processFlights(overviewDoc, world)

        for (offset in 100..lastPageOffset step 100) {
            val urlPaged = if (world) {
                "$url#flights[sort]=reg@filter[country]=RO@flights[start]=$offset"
            } else {
                "$url#flights[start]=$offset"
            }
            log.info("processing: $urlPaged")

            val flightsListDocPaged = Jsoup.parse(http.getJsContent(urlPaged))
            processFlights(flightsListDocPaged, world)

            delay()
        }
    }

    private fun scrapeWithDateFilter(url: String, date: String, world: Boolean) {
        val dateUrl = if (world) {
            "$url#flights[sort]=reg@filter[country]=RO@filter[date]=$date"
        } else {
            "$url#filter[date]=$date"
        }
        log.info("processing date: $date for $url")

        val firstPageDoc = Jsoup.parse(http.getJsContent(dateUrl))
        val lastPageOffset = getLastPageOffset(firstPageDoc, world) ?: 0

        log.info("date $date: last page offset = $lastPageOffset")

        processFlights(firstPageDoc, world)

        for (offset in 100..lastPageOffset step 100) {
            val urlPaged = if (world) {
                "$url#flights[sort]=reg@filter[country]=RO@filter[date]=$date@flights[start]=$offset"
            } else {
                "$url#filter[date]=$date@flights[start]=$offset"
            }
            log.info("processing: $urlPaged")

            val flightsListDocPaged = Jsoup.parse(http.getJsContent(urlPaged))
            processFlights(flightsListDocPaged, world)

            delay()
        }
    }

    private fun extractAvailableDatesWithRetry(url: String, initialDoc: Document, maxRetries: Int = 10): List<String> {
        val dates = extractAvailableDatesFromDoc(initialDoc)
        if (dates.isNotEmpty()) {
            log.info("found ${dates.size} dates for $url")
            return dates
        }

        for (attempt in 1..maxRetries) {
            log.warn("found 0 dates for $url, retrying ($attempt/$maxRetries)...")
            delay(minutes = Random.nextInt(1, 4))

            val retryDoc = Jsoup.parse(http.getJsContent(url))
            val retryDates = extractAvailableDatesFromDoc(retryDoc)
            if (retryDates.isNotEmpty()) {
                log.info("found ${retryDates.size} dates for $url on retry $attempt")
                return retryDates
            }
        }

        error("no dates found for $url after $maxRetries retries but it exceeds the 1000-item cap")
    }

    private fun getLastPageOffset(flightsListDoc: Document, world: Boolean): Int? {
        val needle = if (world) "last page" else "ultima pagină"
        val pgEdges = flightsListDoc.select(".pg-edge")
        for (pgEdge in pgEdges) {
            if (pgEdge.attr("title") == needle) {
                val href = pgEdge.attr("href")
                return href.split("=").lastOrNull()?.toInt()
            }
        }

        return null
    }

    private fun processFlights(flightsListDoc: Document, world: Boolean) {
        val flightsAll = mapFlights(flightsListDoc, world)
        log.info("parsed ${flightsAll.size} flights")

        // fetch the ones we already know
        val existingFlightIds = getExistingFlightIds(flightsAll)
        log.info("found ${existingFlightIds.size} existing flights")

        // excluding those we already know about
        val flights = flightsAll.filterNot { it.id in existingFlightIds }
        log.info("found ${flights.size} new flights")

        // persist
        flights.forEach {
            persist(it)
        }
        log.info("persisted ${flights.size} flights")
    }

    private fun getExistingFlightIds(flights: List<Flight>): Set<Long> {
        val flightIds = flights.map { it.id }.toSet()
        return db.findExistingFlightIds(flightIds)
    }

    private fun persist(flight: Flight) {
        val pilotId = getOrCreate(flight.pilot)
        val takeoffId = getOrCreate(flight.takeoff)
        val gliderId = getOrCreate(flight.glider)

        log.info("trying to persist flight: $flight, pilotId: $pilotId, takeoffId: $takeoffId, gliderId: $gliderId")

        db.persist(
            flight = flight,
            pilotId = pilotId,
            takeoffId = takeoffId,
            gliderId = gliderId,
        )
    }

    private fun getOrCreate(glider: Glider): Long {
        val db = db.findGlider(glider.name)
        if (db != null) {
            return db.id!!
        }
        return this.db.persist(glider)
    }

    private fun getOrCreate(takeoff: Takeoff?): Long? {
        takeoff ?: return null

        if (takeoff.name == "?") {
            return null
        }

        val db = db.findTakeoff(takeoff.name)
        if (db != null) {
            return db.id
        }
        return this.db.persist(takeoff)
    }

    private fun getOrCreate(pilot: Pilot): Long {
        val db = db.findPilot(pilot.username)
        if (db != null) {
            return db.id!!
        }
        return this.db.persist(pilot)
    }

}

private val DATE_PATTERN = Regex("\\d{4}-\\d{2}-\\d{2}")

/**
 * Extract available dates from the date filter dropdown in the rendered HTML.
 *
 * The dropdown is a <select> inside the filter form. The value attribute
 * may be a bare date ("2025-09-30") or include a flight count suffix
 * ("2025-09-30 [10]"). We extract the YYYY-MM-DD portion in either case.
 */
fun extractAvailableDatesFromDoc(doc: Document): List<String> {
    for (select in doc.select("select")) {
        val options = select.select("option")
        val dateValues = options.mapNotNull { option ->
            val value = option.attr("value")
            DATE_PATTERN.find(value)?.value
        }
        if (dateValues.isNotEmpty()) {
            return dateValues
        }
    }
    return emptyList()
}

private fun delay(minutes: Int = Random.nextInt(1, 10)) {
    val sleepMillis = minutes * 60 * 1000L
    log.info("sleeping for $minutes minutes...")
    Thread.sleep(sleepMillis)
}