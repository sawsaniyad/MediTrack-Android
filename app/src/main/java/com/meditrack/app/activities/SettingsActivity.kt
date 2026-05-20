package com.meditrack.app.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.meditrack.app.databinding.ActivitySettingsBinding

/**
 * SettingsActivity - הגדרות אפליקציה (SharedPreferences)
 * אחראית: רע'ד
 */
class SettingsActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "meditrack_prefs"
        const val KEY_REMINDER_MINUTES = "reminder_minutes_before"
        const val KEY_VIBRATION = "vibration_enabled"
        const val KEY_SOUND = "sound_enabled"
        const val DEFAULT_REMINDER_MINUTES = 5
    }

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "הגדרות"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        loadSettings()

        binding.btnSaveSettings.setOnClickListener { saveSettings() }
    }

    private fun loadSettings() {
        val minutes = prefs.getInt(KEY_REMINDER_MINUTES, DEFAULT_REMINDER_MINUTES)
        binding.editReminderMinutes.setText(minutes.toString())
        binding.switchVibration.isChecked = prefs.getBoolean(KEY_VIBRATION, true)
        binding.switchSound.isChecked = prefs.getBoolean(KEY_SOUND, true)
    }

    private fun saveSettings() {
        val minutesText = binding.editReminderMinutes.text.toString()
        val minutes = minutesText.toIntOrNull() ?: DEFAULT_REMINDER_MINUTES

        prefs.edit()
            .putInt(KEY_REMINDER_MINUTES, minutes)
            .putBoolean(KEY_VIBRATION, binding.switchVibration.isChecked)
            .putBoolean(KEY_SOUND, binding.switchSound.isChecked)
            .apply()

        Toast.makeText(this, "הגדרות נשמרו", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
