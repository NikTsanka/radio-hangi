package com.canka.dev.radiohangi.domain.model

/** A country in the World Radio filter dropdown (Radio Browser /countries). */
data class Country(
    val name: String,
    /** ISO 3166-1 alpha-2 code, used as the `countrycode` search param. */
    val code: String,
    val stationCount: Int,
)
