package observer.poleshift.spacealerts

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class SpaceWeatherWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        NotificationHelper.ensureChannel(context)
        WidgetScheduler.start(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Show cached data instantly, then kick a fresh fetch.
        val cached = Prefs.cachedSnapshot(context)
        val views = WidgetUpdater.buildViews(context, cached)
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, views)
        triggerRefresh(context)
        WidgetScheduler.start(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Const.REFRESH_ACTION) {
            triggerRefresh(context)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // If no widgets remain and notifications are off, the scheduler stops itself.
        WidgetScheduler.scheduleNext(context)
    }

    private fun triggerRefresh(context: Context) {
        val i = Intent(context, WidgetAlarmReceiver::class.java).setAction(Const.TICK_ACTION)
        context.sendBroadcast(i)
    }
}
