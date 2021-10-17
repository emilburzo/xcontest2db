package com.emilburzo.service.http

data class BrowserlessContent(
    val url: String,
    val waitFor: Int = 5_000,
    val gotoOptions: BrowserlessGotoOptions = BrowserlessGotoOptions()
)

data class BrowserlessGotoOptions(
    val timeout: Int = 60_000
)