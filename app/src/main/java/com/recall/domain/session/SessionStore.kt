package com.recall.domain.session

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Finished rounds, kept only while the app runs, so the results page can show the one just played. */
class SessionStore {
    private val results = ConcurrentHashMap<String, SessionResult>()

    fun put(result: SessionResult): String = UUID.randomUUID().toString().also { results[it] = result }

    fun get(key: String): SessionResult? = results[key]
}
