package com.emilburzo.main

import com.emilburzo.db.Db
import com.emilburzo.service.Xcontest2Db
import com.emilburzo.service.http.Http
import com.emilburzo.service.rss.Rss

fun main(args: Array<String>) {
    Xcontest2Db(
        db = Db(),
        http = Http(),
        rss = Rss()
    ).run()
}

