package com.emilburzo.service.http

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(Http::class.java)

class Http {

    private val client = HttpClient(CIO) {
        engine { requestTimeout = 120 * 1000 }
        install(ContentNegotiation) { jackson() }
    }

    /**
     * slower, but handles javascript by using headless chrome
     */
    fun getJsContent(url: String, waitFor: String = ".XClist", proxy: String? = null): String {
        return runBlocking {
            val response = client.post(BROWSERLESS_URL) {
                contentType(ContentType.Application.Json)
                setBody(BrowserlessContent(url, waitFor = waitFor, proxy = proxy))
            }
            if (!response.status.isSuccess()) {
                throw RuntimeException("Playwright service returned HTTP ${response.status.value} for $url: ${response.bodyAsText()}")
            }
            response.bodyAsText()
        }
    }

    /**
     * xcontest's API refuses plain requests from our IP, so it has to be fetched
     * through the browser as well — chrome renders the JSON body inside a <pre>.
     */
    fun getJsonContent(url: String, proxy: String? = null): String {
        val content = getJsContent(url, waitFor = "body", proxy = proxy)
        return Jsoup.parse(content).selectFirst("pre")?.wholeText() ?: content
    }

    /**
     * fastest, simple http get, no javascript support
     */
    fun getContent(url: String): String {
        return runBlocking {
            client.get(url).bodyAsText()
        }
    }

    /**
     * Fetch free proxy list from free-proxy-list.net, returning HTTPS-capable proxies
     * as "http://host:port" strings, shuffled randomly.
     */
    fun fetchFreeProxies(): List<String> {
        return try {
            val html = getContent(FREE_PROXY_LIST_URL)
            val proxies = parseFreeProxyList(html)
            log.info("fetched ${proxies.size} HTTPS-capable free proxies")
            proxies.shuffled()
        } catch (e: Exception) {
            log.error("failed to fetch free proxy list: ${e.message}")
            emptyList()
        }
    }
}

private val HOST_PORT = Regex("[\\w.-]+:\\d+")

/**
 * Columns are looked up by their header so that a layout change fails loudly
 * instead of quietly yielding no proxies. SOCKS proxies are listed too, but
 * we only want HTTP ones.
 */
fun parseFreeProxyList(html: String): List<String> {
    val table = Jsoup.parse(html).selectFirst(".fpl-list table")
        ?: error("free proxy list table not found, the page structure has probably changed")

    val headers = table.select("thead th").map { it.text().filterNot(Char::isWhitespace).lowercase() }
    val address = headers.indexOf("ip:port")
    val protocol = headers.indexOf("protocol")
    val https = headers.indexOf("https")
    check(address >= 0 && protocol >= 0 && https >= 0) { "unexpected free proxy list columns: $headers" }

    return table.select("tbody tr").mapNotNull { row ->
        val cells = row.select("td").map { it.text().filterNot(Char::isWhitespace) }
        val hostPort = cells.getOrNull(address)?.takeIf { HOST_PORT.matches(it) }
        val usable = cells.getOrNull(protocol).equals("http", ignoreCase = true) &&
                cells.getOrNull(https).equals("yes", ignoreCase = true)

        if (usable && hostPort != null) "http://$hostPort" else null
    }
}
