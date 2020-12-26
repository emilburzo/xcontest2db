package com.emilburzo.service.http

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*


//private val httpClient = HttpClient(CIO) {
//    install(JsonFeature) {
//        serializer = JacksonSerializer()
//    }
//}

class Http {

    suspend fun getContent(url: String): String {
        HttpClient().use { client ->
            return client.get(url)
        }
    }

}