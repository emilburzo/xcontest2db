package com.emilburzo.service.http

data class BrowserlessContent(
    val url: String,
    val waitFor: String = ".XClist",
    val gotoOptions: BrowserlessGotoOptions = BrowserlessGotoOptions(),
    val proxy: String? = null,
)

data class BrowserlessGotoOptions(
    val timeout: Int = 60_000
)

