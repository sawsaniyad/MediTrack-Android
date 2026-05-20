package com.meditrack.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.meditrack.app.R
import com.meditrack.app.adapters.MedicationAdapter
import com.meditrack.app.database.DatabaseHelper
import com.meditrack.app.database.MedicationDAO
import com.meditrack.app.databinding.ActivityMedicationListBinding
import com.meditrack.app.models.Medication
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * MedicationListActivity - מסך ראשי: רשימת תרופות
 * אחראית: סוסאן
 */
class MedicationListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMedicationListBinding
    private lateinit var adapter: MedicationAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var medicationDAO: MedicationDAO
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicationListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "MediTrack - התרופות שלי"

        dbHelper = DatabaseHelper(this)
        medicationDAO = MedicationDAO(dbHelper)

        setupRecyclerView()

        binding.fabAddMedication.setOnClickListener {
            startActivity(Intent(this, AddEditMedicationActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadMedications()
    }

    private fun setupRecyclerView() {
        adapter = MedicationAdapter(
            onEdit = { med ->
                val intent = Intent(this, AddEditMedicationActivity::class.java)
                intent.putExtra(AddEditMedicationActivity.EXTRA_MED_ID, med.id)
                startActivity(intent)
            },
            onDelete = { med -> confirmDelete(med) }
        )
        binding.recyclerMedications.layoutManager = LinearLayoutManager(this)
        binding.recyclerMedications.adapter = adapter
    }

    private fun loadMedications() {
        executor.execute {
            val meds = medicationDAO.getActive()
            runOnUiThread {
                adapter.submitList(meds)
                binding.textEmpty.visibility = if (meds.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun confirmDelete(med: Medication) {
        AlertDialog.Builder(this)
            .setTitle("מחיקת תרופה")
            .setMessage("האם למחוק את ${med.name}?")
            .setPositiveButton("מחק") { _, _ ->
                executor.execute {
                    medicationDAO.delete(med.id)
                    loadMedications()
                }
            }
            .setNegativeButton("ביטול", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            R.id.action_exit -> {
                finishAffinity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("אודות MediTrack")
            .setMessage("מנהל תרופות חכם\n\nגרסה 1.0\n\nפותח על ידי:\nסמירה | רע'ד | סוסאן\n\nפרויקט גמר - מכללת אזריאלי")
            .setPositiveButton("סגור", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
        dbHelper.close()
    }
}
