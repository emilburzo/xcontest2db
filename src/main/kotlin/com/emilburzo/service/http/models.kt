package com.emilburzo.service.http

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Browserless validates its request schema strictly and rejects `proxy` outright,
 * so the field is only sent when we actually have a proxy to pass.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class BrowserlessContent(
    val url: String,
    val waitFor: String = ".XClist",
    val gotoOptions: BrowserlessGotoOptions = BrowserlessGotoOptions(),
    val proxy: String? = null,
)

data class BrowserlessGotoOptions(
    val timeout: Int = 60_000
)

