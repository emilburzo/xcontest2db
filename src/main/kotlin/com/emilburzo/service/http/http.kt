package com.emilburzo.service.http

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking


class Http {

    /**
     * slower, but handles javascript by using headless chrome
     */
    fun getJsContent(url: String): String {
        return runBlocking {
            HttpClient(CIO) {
                engine { requestTimeout = 60 * 1000 }
                install(JsonFeature)
            }.use { client ->
                client.post<String> {
                    url(BROWSERLESS_URL)
                    contentType(ContentType.Application.Json)
                    body = BrowserlessContent(url)
                }
            }
        }
    }

    /**
     * fastest, simple http get, no javascript support
     */
    fun getContent(url: String): String {
        return runBlocking {
            HttpClient().use { client ->
                client.get(url)
            }
        }
    }
}