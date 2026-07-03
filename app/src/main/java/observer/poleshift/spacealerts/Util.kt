package observer.poleshift.spacealerts

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Const {
    const val XRAY_LATEST = "https://services.swpc.noaa.gov/json/goes/primary/xray-flares-latest.json"
    const val XRAY_7DAY   = "https://services.swpc.noaa.gov/json/goes/primary/xray-flares-7-day.json"
    const val SDO_FLARE_IMG = "https://sdo.gsfc.nasa.gov/assets/img/latest/latest_512_0131.jpg"

    const val CHANNEL_ID = "space_weather_flare_alerts"
    const val TICK_ACTION = "observer.poleshift.spacealerts.TICK"
    const val REFRESH_ACTION = "observer.poleshift.spacealerts.REFRESH"
    const val UPDATE_INTERVAL_MS = 60_000L
    const val NOTIF_FLARE_ID = 2001
    const val NOTIF_TEST_ID = 2002
}

object Flare {
    private val ORDER = mapOf('A' to 1, 'B' to 2, 'C' to 3, 'M' to 4, 'X' to 5)

    /** Rank of a flare class letter (A=1 .. X=5); 0 if unknown. */
    fun rank(cls: String?): Int {
        if (cls.isNullOrBlank()) return 0
        val c = cls.trim().uppercase()[0]
        return ORDER[c] ?: 0
    }

    fun isMplus(cls: String?): Boolean = rank(cls) >= 4

    /** RemoteViews-safe color int for a flare class. */
    fun color(cls: String?): Int {
        return when (rank(cls)) {
            5 -> Color.parseColor("#FF2B4E") // X
            4 -> Color.parseColor("#FFB400") // M
            3 -> Color.parseColor("#00FF62") // C
            1, 2 -> Color.parseColor("#0B8F3C") // A/B
            else -> Color.parseColor("#4F9C6A")
        }
    }
}

object TimeUtil {
    private val PATTERNS = arrayOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss"
    )

    fun parseMillis(s: String?): Long? {
        if (s.isNullOrBlank()) return null
        for (p in PATTERNS) {
            try {
                val fmt = SimpleDateFormat(p, Locale.US)
                fmt.timeZone = TimeZone.getTimeZone("UTC")
                return fmt.parse(s)?.time
            } catch (_: Exception) { /* try next */ }
        }
        return null
    }

    /** Format an ISO timestamp to local "MMM dd HH:mm". */
    fun localShort(s: String?): String {
        val ms = parseMillis(s) ?: return s ?: "—"
        val out = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
        return out.format(Date(ms))
    }

    fun clockNow(): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}

object Net {
    /** Blocking bitmap download (call off the main thread). */
    fun loadBitmap(urlStr: String): Bitmap? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 15000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SpaceWeatherAlerts/1.0")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.use { BitmapFactory.decodeStream(it) }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    /** Blocking text download (call off the main thread). */
    fun loadText(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 15000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SpaceWeatherAlerts/1.0")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
