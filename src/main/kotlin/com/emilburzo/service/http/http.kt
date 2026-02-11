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
    fun getJsContent(url: String, proxy: String? = null): String {
        return runBlocking {
            val response = client.post(BROWSERLESS_URL) {
                contentType(ContentType.Application.Json)
                setBody(BrowserlessContent(url, proxy = proxy))
            }
            if (!response.status.isSuccess()) {
                throw RuntimeException("Playwright service returned HTTP ${response.status.value} for $url: ${response.bodyAsText()}")
            }
            response.bodyAsText()
        }
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
            val html = getContent("https://free-proxy-list.net/en/")
            val proxies = parseFreeProxyList(html)
            log.info("fetched ${proxies.size} HTTPS-capable free proxies")
            proxies.shuffled()
        } catch (e: Exception) {
            log.error("failed to fetch free proxy list: ${e.message}")
            emptyList()
        }
    }
}

fun parseFreeProxyList(html: String): List<String> {
    val doc = Jsoup.parse(html)
    val proxies = mutableListOf<String>()

    for (row in doc.select(".fpl-list table tbody tr")) {
        val cells = row.select("td")
        if (cells.size < 8) continue

        val ip = cells[0].text().trim()
        val port = cells[1].text().trim()
        val https = cells[6].text().trim().lowercase()

        if (https == "yes" && ip.isNotEmpty() && port.isNotEmpty()) {
            proxies.add("http://$ip:$port")
        }
    }

    return proxies
}
