package com.canka.dev.radiohangi.domain.model

/**
 * The flag for an ISO 3166-1 alpha-2 [code] as a Unicode emoji, built by mapping each ASCII
 * letter to its regional-indicator symbol. Empty when [code] isn't a valid 2-letter code.
 */
fun isoFlag(code: String?): String {
    val c = code?.uppercase() ?: return ""
    if (c.length != 2 || c[0] !in 'A'..'Z' || c[1] !in 'A'..'Z') return ""
    val base = 0x1F1E6 // 🇦 — regional indicator symbol letter A
    return String(Character.toChars(base + (c[0] - 'A'))) +
        String(Character.toChars(base + (c[1] - 'A')))
}

/** A country in the World Radio filter dropdown (Radio Browser /countries). */
data class Country(
    val name: String,
    /** ISO 3166-1 alpha-2 code, used as the `countrycode` search param. */
    val code: String,
    val stationCount: Int,
) {
    /** The country's flag emoji (empty when [code] isn't a valid 2-letter ISO code). */
    val flag: String get() = isoFlag(code)

    /** "[flag]  " when a flag is available, otherwise an empty string (for label prefixes). */
    val flagPrefix: String get() = if (flag.isEmpty()) "" else "$flag  "
}
