package observer.poleshift.spacealerts

import android.content.Context

object Prefs {
    private const val FILE = "space_alerts_prefs"
    private const val K_NOTIF = "notifications_enabled"
    private const val K_LAST_NOTIFIED = "last_notified_flare_id"
    private const val K_CUR_CLASS = "cur_class"
    private const val K_CUR_TIME = "cur_time"
    private const val K_LASTM_CLASS = "lastm_class"
    private const val K_LASTM_TIME = "lastm_time"

    private fun sp(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun notificationsEnabled(ctx: Context): Boolean =
        sp(ctx).getBoolean(K_NOTIF, true)

    fun setNotificationsEnabled(ctx: Context, v: Boolean) =
        sp(ctx).edit().putBoolean(K_NOTIF, v).apply()

    fun lastNotifiedId(ctx: Context): String? =
        sp(ctx).getString(K_LAST_NOTIFIED, null)

    fun setLastNotifiedId(ctx: Context, id: String) =
        sp(ctx).edit().putString(K_LAST_NOTIFIED, id).apply()

    fun cacheSnapshot(ctx: Context, snap: Snapshot) {
        sp(ctx).edit()
            .putString(K_CUR_CLASS, snap.currentClass)
            .putString(K_CUR_TIME, snap.currentTime)
            .putString(K_LASTM_CLASS, snap.lastMClass)
            .putString(K_LASTM_TIME, snap.lastMTime)
            .apply()
    }

    fun cachedSnapshot(ctx: Context): Snapshot {
        val s = sp(ctx)
        return Snapshot(
            currentClass = s.getString(K_CUR_CLASS, null),
            currentTime = s.getString(K_CUR_TIME, null),
            satellite = null,
            lastMClass = s.getString(K_LASTM_CLASS, null),
            lastMTime = s.getString(K_LASTM_TIME, null)
        )
    }
}
