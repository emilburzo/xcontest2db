package com.emilburzo.main

import com.emilburzo.db.Db
import com.emilburzo.service.Xcontest2Db
import com.emilburzo.service.http.Http

fun main() {
    val mode = System.getenv("FETCH_MODE") ?: "recent"
    val xcontest = Xcontest2Db(db = Db(), http = Http())

    when (mode) {
        "all" -> xcontest.fetchAll()
        "recent" -> xcontest.fetchRecent()
        else -> error("Unknown FETCH_MODE: $mode (expected 'recent' or 'all')")
    }
}

