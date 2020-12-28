package com.emilburzo.main

import com.emilburzo.db.Db
import com.emilburzo.service.Xcontest2Db
import com.emilburzo.service.http.Http

fun main() {
    // todo add option to choose mode
    Xcontest2Db(db = Db(), http = Http()).fetchRecent()
//    Xcontest2Db(db = Db(), http = Http()).fetchAll()
}

