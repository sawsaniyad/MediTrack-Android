package com.meditrack.app.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.meditrack.app.adapters.HistoryAdapter
import com.meditrack.app.database.DatabaseHelper
import com.meditrack.app.database.IntakeLogDAO
import com.meditrack.app.databinding.ActivityHistoryBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * HistoryActivity - היסטוריית נטילות עם סינון
 * אחראית: רע'ד
 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var logDAO: IntakeLogDAO
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "היסטוריית נטילות"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dbHelper = DatabaseHelper(this)
        logDAO = IntakeLogDAO(dbHelper)

        adapter = HistoryAdapter()
        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistory.adapter = adapter

        binding.btnFilterToday.setOnClickListener { loadTodayLogs() }
        binding.btnFilterAll.setOnClickListener { loadAllLogs() }

        loadTodayLogs()
    }

    private fun loadTodayLogs() {
        executor.execute {
            val logs = logDAO.getTodayLogs()
            runOnUiThread {
                adapter.submitList(logs)
                binding.textHistoryEmpty.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun loadAllLogs() {
        executor.execute {
            val logs = logDAO.getByDateRange(0, Long.MAX_VALUE)
            runOnUiThread {
                adapter.submitList(logs)
                binding.textHistoryEmpty.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
        dbHelper.close()
    }
}
