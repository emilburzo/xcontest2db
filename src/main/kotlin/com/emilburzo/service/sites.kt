package com.emilburzo.service

import com.emilburzo.service.http.Http
import com.fasterxml.jackson.databind.ObjectMapper
import net.postgis.jdbc.geometry.Point
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(Sites::class.java)

private val mapper = ObjectMapper()

/**
 * xcontest's own site database. Since 2026 the flights list links takeoffs with
 * a registered site to the hot-spots page, which carries no coordinates, so this
 * is the only remaining source for the coordinates of a takeoff we have never
 * seen before.
 *
 * Sites are looked up one at a time and cached for the lifetime of the run —
 * in practice this is rarely hit at all, since anything we have scraped before
 * is already in our own takeoffs table.
 */
class Sites(private val http: Http) {

    private val cache = mutableMapOf<Long, Point?>()

    fun findPosition(siteId: Long, volume: String, proxy: String? = null): Point? {
        if (cache.containsKey(siteId)) {
            return cache[siteId]
        }

        val position = fetchPosition(siteId, volume, proxy)
        cache[siteId] = position
        return position
    }

    private fun fetchPosition(siteId: Long, volume: String, proxy: String?): Point? {
        val url = "$SITES_API_URL/$volume:$siteId&lng=en&key=$XCONTEST_API_KEY"

        return try {
            parseSitePosition(http.getJsonContent(url, proxy = proxy))
        } catch (e: Exception) {
            log.error("failed to look up site $siteId: ${e.message}")
            null
        }
    }
}

/**
 * Expects a single site as returned by the sites API:
 * `{"id":4906,"name":"Sticlaria",...,"position":{"longitude":26.84863,"latitude":47.4157,"altitude":412},...}`
 */
fun parseSitePosition(json: String): Point? {
    val root = mapper.readTree(json)

    val error = root.path("error").path("message")
    if (!error.isMissingNode) {
        log.warn("sites API returned an error: ${error.asText()}")
        return null
    }

    val position = root.path("position")
    val longitude = position.path("longitude")
    val latitude = position.path("latitude")
    if (longitude.isMissingNode || latitude.isMissingNode) {
        log.warn("sites API response has no position: ${json.take(200)}")
        return null
    }

    return Point(longitude.asDouble(), latitude.asDouble())
}
