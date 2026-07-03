package observer.poleshift.spacealerts

import org.json.JSONArray
import org.json.JSONObject

/** Immutable snapshot rendered by the widget and used for notifications. */
data class Snapshot(
    val currentClass: String?,
    val currentTime: String?,
    val satellite: String?,
    val lastMClass: String?,
    val lastMTime: String?
) {
    /** Stable id for the last M+ flare, used to deduplicate notifications. */
    val lastMId: String?
        get() = if (lastMClass != null && lastMTime != null) "$lastMTime|$lastMClass" else null
}

object SpaceWeatherRepo {

    /** Blocking fetch. Must be called from a background thread / coroutine on IO. */
    fun fetch(): Snapshot {
        val latest = parseLatest(Net.loadText(Const.XRAY_LATEST))
        val lastM = parseLastMplus(Net.loadText(Const.XRAY_7DAY))

        // If the current record itself is an M+ flare and is newer than the 7-day pick, prefer it.
        val current = latest
        var lastMClass = lastM?.first
        var lastMTime = lastM?.second

        if (current != null && Flare.isMplus(current.maxClass)) {
            val curMs = TimeUtil.parseMillis(current.maxTime) ?: 0L
            val lastMs = TimeUtil.parseMillis(lastMTime) ?: -1L
            if (curMs >= lastMs) {
                lastMClass = current.maxClass
                lastMTime = current.maxTime
            }
        }

        return Snapshot(
            currentClass = current?.currentClass ?: current?.maxClass,
            currentTime = current?.timeTag ?: current?.maxTime,
            satellite = current?.satellite,
            lastMClass = lastMClass,
            lastMTime = lastMTime
        )
    }

    private data class Latest(
        val currentClass: String?,
        val maxClass: String?,
        val maxTime: String?,
        val timeTag: String?,
        val satellite: String?
    )

    private fun parseLatest(json: String?): Latest? {
        if (json.isNullOrBlank()) return null
        return try {
            val obj: JSONObject = when {
                json.trim().startsWith("[") -> {
                    val arr = JSONArray(json)
                    if (arr.length() == 0) return null
                    arr.getJSONObject(0)
                }
                else -> JSONObject(json)
            }
            Latest(
                currentClass = obj.optStringOrNull("current_class"),
                maxClass = obj.optStringOrNull("max_class"),
                maxTime = obj.optStringOrNull("max_time"),
                timeTag = obj.optStringOrNull("time_tag") ?: obj.optStringOrNull("current_time"),
                satellite = if (obj.has("satellite")) obj.opt("satellite")?.toString() else null
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Returns Pair(class, maxTime) of the most recent M-or-greater flare in the 7-day file. */
    private fun parseLastMplus(json: String?): Pair<String, String>? {
        if (json.isNullOrBlank()) return null
        return try {
            val arr = JSONArray(json)
            var bestClass: String? = null
            var bestTime: String? = null
            var bestMs = Long.MIN_VALUE
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val cls = o.optStringOrNull("max_class") ?: continue
                if (!Flare.isMplus(cls)) continue
                val t = o.optStringOrNull("max_time") ?: o.optStringOrNull("begin_time")
                val ms = TimeUtil.parseMillis(t) ?: continue
                if (ms > bestMs) {
                    bestMs = ms; bestClass = cls; bestTime = t
                }
            }
            if (bestClass != null && bestTime != null) Pair(bestClass, bestTime) else null
        } catch (_: Exception) {
            null
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val v = optString(key, "")
        return if (v.isBlank() || v == "null") null else v
    }
}
