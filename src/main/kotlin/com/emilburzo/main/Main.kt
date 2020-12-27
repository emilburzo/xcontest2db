package com.emilburzo.main

import com.emilburzo.db.Db
import com.emilburzo.service.Xcontest2Db
import com.emilburzo.service.http.Http

fun main() {
    Xcontest2Db(
        db = Db(),
        http = Http(),
    ).fetchRecent()
}

