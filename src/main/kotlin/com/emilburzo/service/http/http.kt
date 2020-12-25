package com.emilburzo.service.http

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*


/**
 * Created by emil on 07.12.2019.
 */

val client = HttpClient() {
    install(JsonFeature) {
        serializer = JaGsonSerializer {
            // Configurable .GsonBuilder
            serializeNulls()
            disableHtmlEscaping()
        }
    }
}

class Http {

    fun getContent(url: String): String? {
        val request = Request.Builder().url(url).build()
        return httpClient.newCall(request).execute().body?.string()
    }
}