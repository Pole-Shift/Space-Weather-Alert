package observer.poleshift.spacealerts

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

class SplashActivity : AppCompatActivity() {

    private val minShowMs = 1400L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Make sure the alert channel and 60s widget loop are ready early.
        NotificationHelper.ensureChannel(this)
        WidgetScheduler.start(this)

        val img = findViewById<ImageView>(R.id.splash_img)
        val progress = findViewById<ProgressBar>(R.id.splash_progress)
        val startedAt = System.currentTimeMillis()

        lifecycleScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                Net.loadBitmap(getString(R.string.splash_url))
            }
            if (bmp != null) img.setImageBitmap(bmp)
            progress.visibility = View.GONE

            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < minShowMs) {
                withContext(Dispatchers.IO) { Thread.sleep(minShowMs - elapsed) }
            }
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}
