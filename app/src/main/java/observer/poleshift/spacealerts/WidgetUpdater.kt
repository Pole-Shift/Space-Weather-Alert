package observer.poleshift.spacealerts

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

object WidgetUpdater {

    fun widgetIds(ctx: Context): IntArray {
        val mgr = AppWidgetManager.getInstance(ctx)
        return mgr.getAppWidgetIds(ComponentName(ctx, SpaceWeatherWidgetProvider::class.java))
    }

    fun updateAll(ctx: Context, snap: Snapshot) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = widgetIds(ctx)
        if (ids.isEmpty()) return
        val views = buildViews(ctx, snap)
        for (id in ids) mgr.updateAppWidget(id, views)
    }

    fun buildViews(ctx: Context, snap: Snapshot): RemoteViews {
        val rv = RemoteViews(ctx.packageName, R.layout.widget_space_weather)

        val cur = snap.currentClass ?: "--"
        rv.setTextViewText(R.id.w_flux_class, cur)
        rv.setTextColor(R.id.w_flux_class, Flare.color(snap.currentClass))

        rv.setTextViewText(R.id.w_lastm_class, snap.lastMClass ?: "none")
        rv.setTextColor(R.id.w_lastm_class, Flare.color(snap.lastMClass))
        rv.setTextViewText(
            R.id.w_lastm_time,
            if (snap.lastMTime != null) TimeUtil.localShort(snap.lastMTime) else "no M+ in 7 days"
        )

        rv.setTextViewText(R.id.w_updated, "updated ${TimeUtil.clockNow()}")

        rv.setOnClickPendingIntent(R.id.w_root, openApp(ctx))
        rv.setOnClickPendingIntent(R.id.w_refresh, refresh(ctx))
        return rv
    }

    private fun openApp(ctx: Context): PendingIntent {
        val i = Intent(ctx, SplashActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            ctx, 10, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun refresh(ctx: Context): PendingIntent {
        val i = Intent(ctx, SpaceWeatherWidgetProvider::class.java)
            .setAction(Const.REFRESH_ACTION)
        return PendingIntent.getBroadcast(
            ctx, 11, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
