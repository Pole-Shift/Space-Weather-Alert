package observer.poleshift.spacealerts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        NotificationHelper.ensureChannel(this)

        val sw = findViewById<SwitchCompat>(R.id.switch_notifications)
        sw.isChecked = Prefs.notificationsEnabled(this)
        sw.setOnCheckedChangeListener { _, checked ->
            Prefs.setNotificationsEnabled(this, checked)
            if (checked) {
                requestNotifPermissionIfNeeded()
                WidgetScheduler.start(this)
            } else {
                WidgetScheduler.scheduleNext(this) // stops loop if no widget present
            }
        }

        findViewById<Button>(R.id.btn_test).setOnClickListener {
            Toast.makeText(this, "Sending test notification…", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                val img = withContext(Dispatchers.IO) { Net.loadBitmap(Const.SDO_FLARE_IMG) }
                NotificationHelper.showTest(this@SettingsActivity, img)
            }
        }

        findViewById<Button>(R.id.btn_update_widget).setOnClickListener {
            sendBroadcast(
                Intent(this, WidgetAlarmReceiver::class.java).setAction(Const.TICK_ACTION)
            )
            Toast.makeText(this, "Widget refresh requested", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_system_settings).setOnClickListener {
            val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(i)
        }
    }

    private fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }
}
