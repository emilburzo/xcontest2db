package com.emilburzo.service

const val FLIGHTS_RECENT_URL_WORLD = "https://www.xcontest.org/world/en/flights/#filter[country]=RO@filter[country]=RO@flights[sort]=reg"
const val FLIGHTS_RECENT_URL_ROMANIA = "https://www.xcontest.org/romania/zboruri/"

val TIMEZONE: String = System.getenv("TZ") ?: "Europe/Bucharest"