package com.canka.dev.radiohangi.data

/**
 * Single source of truth for every external endpoint and tunable constant, taken
 * verbatim from the existing web version. Feature code (M3–M5) reads from here so
 * URLs/params are never duplicated across the codebase.
 */
object AppConfig {

    // ---- Screen A: Main Radio (fixed Zeno.fm stream) ----------------------------------
    /** Zeno mount id; building block for the stream + metadata URLs below. */
    const val ZENO_MOUNT = "bjclt64fhc4uv"

    /** Audio stream (MP3/AAC over HTTP). */
    const val ZENO_STREAM_URL = "https://stream.zeno.fm/$ZENO_MOUNT"

    /** Server-Sent Events now-playing metadata (each event JSON has a `streamTitle`). */
    const val ZENO_METADATA_SSE_URL =
        "https://api.zeno.fm/mounts/metadata/subscribe/$ZENO_MOUNT"

    /** Auto-reconnect delay after an SSE error. */
    const val SSE_RECONNECT_DELAY_MS = 5_000L

    // ---- Album art (Deezer) ------------------------------------------------------------
    /** Normal JSON GET (NO jsonp/callback). Cover read from `data[0].album.cover_big`. */
    const val DEEZER_SEARCH_URL = "https://api.deezer.com/search"

    // ---- Lyrics (lyrics.ovh) -----------------------------------------------------------
    /** GET {base}/{artist}/{song} → JSON `{ "lyrics": "..." }`. */
    const val LYRICS_BASE_URL = "https://api.lyrics.ovh/v1/"

    // ---- Screen B: World Radio (Radio Browser) -----------------------------------------
    const val RADIO_BROWSER_BASE_URL = "https://de1.api.radio-browser.info/json/"

    /** Hide countries with fewer than this many stations. */
    const val MIN_STATIONS_PER_COUNTRY = 5

    /** Max genre chips to fetch from /tags. */
    const val TAGS_LIMIT = 80

    /** "Load more" page size for station search. */
    const val STATIONS_PAGE_SIZE = 48

    /** Search input debounce. */
    const val SEARCH_DEBOUNCE_MS = 500L

    // ---- Favorites persistence (World Radio only) --------------------------------------
    /** DataStore file + key names mirror the web localStorage keys. */
    const val FAVORITES_DATASTORE = "wr_favs"
    const val FAVORITES_KEY = "wr-favs-data"

    // ---- Recently-played stations ------------------------------------------------------
    const val RECENTS_DATASTORE = "wr_recents"
    const val RECENTS_KEY = "wr-recents-data"

    /** Max recently-played stations to remember (newest first). */
    const val RECENTS_SIZE = 12

    /** Max stations shown in the Radio-screen quick-play row (favorites + recents, deduped). */
    const val QUICK_ROW_SIZE = 12

    /** Max stations pushed to the home-screen widget's quick-play row. */
    const val WIDGET_QUICK_SIZE = 4

    // ---- Misc --------------------------------------------------------------------------
    const val SHARE_TEMPLATE = "Now listening to: %1\$s by %2\$s on Radio Hangi"
}
