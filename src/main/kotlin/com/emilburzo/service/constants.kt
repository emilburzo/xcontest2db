package com.emilburzo.service

const val FLIGHTS_RECENT_URL_WORLD = "https://www.xcontest.org/world/en/flights/#filter[country]=RO@filter[country]=RO@flights[sort]=reg"
const val FLIGHTS_RECENT_URL_ROMANIA = "https://www.xcontest.org/romania/zboruri/"

const val SITES_API_URL = "https://www.xcontest.org/api/data/?sites/world"

// static across the site, see XContest.API_KEY
const val XCONTEST_API_KEY = "03ECF5952EB046AC-A53195E89B7996E4-D1B128E82C3E2A66"

val TIMEZONE: String = System.getenv("TZ") ?: "Europe/Bucharest"