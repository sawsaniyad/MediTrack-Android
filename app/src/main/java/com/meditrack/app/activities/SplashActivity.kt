package com.meditrack.app.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.meditrack.app.databinding.ActivitySplashBinding
import com.meditrack.app.notifications.NotificationHelper

/**
 * SplashActivity - מסך פתיחה (2.5 שניות)
 * אחראית: סמירה
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // צור ערוץ התראות
        NotificationHelper.createNotificationChannel(this)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MedicationListActivity::class.java))
            finish()
        }, 2500)
    }
}
