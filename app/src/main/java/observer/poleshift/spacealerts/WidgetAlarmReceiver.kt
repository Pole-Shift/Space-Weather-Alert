package observer.poleshift.spacealerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appCtx = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snap = SpaceWeatherRepo.fetch()
                Prefs.cacheSnapshot(appCtx, snap)
                WidgetUpdater.updateAll(appCtx, snap)
                checkForFlareAlert(appCtx, snap)
            } catch (_: Exception) {
                // keep the loop alive; use last cached snapshot for the widget
                WidgetUpdater.updateAll(appCtx, Prefs.cachedSnapshot(appCtx))
            } finally {
                WidgetScheduler.scheduleNext(appCtx)
                pending.finish()
            }
        }
    }

    private fun checkForFlareAlert(ctx: Context, snap: Snapshot) {
        if (!Prefs.notificationsEnabled(ctx)) return
        val id = snap.lastMId ?: return
        if (!Flare.isMplus(snap.lastMClass)) return

        val previous = Prefs.lastNotifiedId(ctx)
        if (previous == null) {
            // First run: seed current state, don't alert on an already-past flare.
            Prefs.setLastNotifiedId(ctx, id)
            return
        }
        if (id != previous) {
            val img = Net.loadBitmap(Const.SDO_FLARE_IMG)
            NotificationHelper.showFlareAlert(ctx, snap, img)
            Prefs.setLastNotifiedId(ctx, id)
        }
    }
}
