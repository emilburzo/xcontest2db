package com.emilburzo.main

import com.emilburzo.db.Db
import com.emilburzo.service.Xcontest2Db
import com.emilburzo.service.http.Http

fun main() {
    val mode = System.getenv("FETCH_MODE") ?: "recent"
    val xcontest = Xcontest2Db(db = Db(), http = Http())

    when (mode) {
        "recent" -> xcontest.fetchRecent()
        "populate" -> xcontest.populate()
        "scrape" -> xcontest.scrape()
        else -> error("Unknown FETCH_MODE: $mode (expected 'recent', 'populate', or 'scrape')")
    }
}

