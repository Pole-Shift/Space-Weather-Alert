package observer.poleshift.spacealerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object WidgetScheduler {

    private fun pendingIntent(ctx: Context): PendingIntent {
        val i = Intent(ctx, WidgetAlarmReceiver::class.java).setAction(Const.TICK_ACTION)
        return PendingIntent.getBroadcast(
            ctx, 1001, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** True if we should keep ticking: a widget exists OR notifications are enabled. */
    private fun shouldRun(ctx: Context): Boolean =
        WidgetUpdater.widgetIds(ctx).isNotEmpty() || Prefs.notificationsEnabled(ctx)

    fun start(ctx: Context) = scheduleNext(ctx)

    fun scheduleNext(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(ctx)

        if (!shouldRun(ctx)) {
            am.cancel(pi)
            return
        }

        val triggerAt = System.currentTimeMillis() + Const.UPDATE_INTERVAL_MS
        val canExact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                // Fall back to an inexact allow-while-idle alarm if exact isn't permitted.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pendingIntent(ctx))
    }
}
