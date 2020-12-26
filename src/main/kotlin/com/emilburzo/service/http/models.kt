package com.emilburzo.service.http

data class BrowserlessContent(
    val url: String,
    val waitFor: Int = 5000
)