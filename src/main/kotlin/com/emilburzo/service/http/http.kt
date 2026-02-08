package com.emilburzo.service.http

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.runBlocking


class Http {

    /**
     * slower, but handles javascript by using headless chrome
     */
    fun getJsContent(url: String): String {
        return runBlocking {
            HttpClient(CIO) {
                engine { requestTimeout = 60 * 1000 }
                install(ContentNegotiation) { jackson() }
            }.use { client ->
                client.post(BROWSERLESS_URL) {
                    contentType(ContentType.Application.Json)
                    setBody(BrowserlessContent(url))
                }.bodyAsText()
            }
        }
    }

    /**
     * fastest, simple http get, no javascript support
     */
    fun getContent(url: String): String {
        return runBlocking {
            HttpClient().use { client ->
                client.get(url).bodyAsText()
            }
        }
    }
}
