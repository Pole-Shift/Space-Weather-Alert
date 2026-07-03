package observer.poleshift.spacealerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    fun ensureChannel(ctx: Context) {
        val mgr = ctx.getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(Const.CHANNEL_ID) != null) return

        val soundUri = Uri.parse("android.resource://${ctx.packageName}/${R.raw.not}")
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            Const.CHANNEL_ID,
            "Solar flare alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "M-class and greater solar flare alerts"
            setSound(soundUri, attrs)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            enableLights(true)
        }
        mgr.createNotificationChannel(channel)
    }

    private fun openAppIntent(ctx: Context): PendingIntent {
        val i = Intent(ctx, SplashActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            ctx, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Post an alert for an M-or-greater flare, with the latest SDO AIA 131 image. */
    fun showFlareAlert(ctx: Context, snap: Snapshot, image: Bitmap?) {
        ensureChannel(ctx)
        val cls = snap.lastMClass ?: "M?"
        val when_ = TimeUtil.localShort(snap.lastMTime)

        val builder = NotificationCompat.Builder(ctx, Const.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$cls-class solar flare")
            .setContentText("Peaked $when_ · current flux ${snap.currentClass ?: "--"}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx))

        if (image != null) {
            builder.setLargeIcon(image)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(image)
                        .bigLargeIcon(null as Bitmap?)
                        .setSummaryText("SDO AIA 131 Å · $cls at $when_")
                )
        }
        safeNotify(ctx, Const.NOTIF_FLARE_ID, builder)
    }

    fun showTest(ctx: Context, image: Bitmap?) {
        ensureChannel(ctx)
        val builder = NotificationCompat.Builder(ctx, Const.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Test alert · Space Weather Alerts")
            .setContentText("Notifications and sound are working.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx))
        if (image != null) {
            builder.setLargeIcon(image)
                .setStyle(NotificationCompat.BigPictureStyle().bigPicture(image)
                    .setSummaryText("SDO AIA 131 Å (live sample)"))
        }
        safeNotify(ctx, Const.NOTIF_TEST_ID, builder)
    }

    private fun safeNotify(ctx: Context, id: Int, builder: NotificationCompat.Builder) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        try {
            nm.notify(id, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted (Android 13+)
        }
    }
}
