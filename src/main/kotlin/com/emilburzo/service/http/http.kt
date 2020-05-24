package com.emilburzo.service.http

import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Created by emil on 07.12.2019.
 */

val httpClient = OkHttpClient()

class Http {

    fun getContent(url: String): String? {
        val request = Request.Builder().url(url).build()
        return httpClient.newCall(request).execute().body?.string()
    }
}