package com.emilburzo.service.http

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking


class Http {

    fun getJsContent(url: String): String {
        return runBlocking {
            HttpClient() {
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

    fun getContent(url: String): String {
        return runBlocking {
            HttpClient().use { client ->
                client.get(url)
            }
        }
    }
}