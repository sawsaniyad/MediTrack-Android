package com.meditrack.app.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.meditrack.app.database.DatabaseHelper
import com.meditrack.app.database.MedicationDAO
import com.meditrack.app.database.ScheduleDAO
import com.meditrack.app.databinding.ActivityAddEditMedicationBinding
import com.meditrack.app.models.Medication
import com.meditrack.app.models.Schedule
import com.meditrack.app.utils.AlarmScheduler
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * AddEditMedicationActivity - הוספה / עריכת תרופה
 * אחראית: סוסאן
 */
class AddEditMedicationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MED_ID = "extra_med_id"
        private const val REQ_CAMERA = 101
        private const val REQ_CONTACT = 102
    }

    private lateinit var binding: ActivityAddEditMedicationBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var medDAO: MedicationDAO
    private lateinit var schDAO: ScheduleDAO
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private var editMedId: Long = -1L
    private var savedPhotoPath: String = ""
    private var contactName: String = ""
    private var contactPhone: String = ""

    // CameraX
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditMedicationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        medDAO = MedicationDAO(dbHelper)
        schDAO = ScheduleDAO(dbHelper)

        editMedId = intent.getLongExtra(EXTRA_MED_ID, -1L)
        supportActionBar?.title = if (editMedId == -1L) "הוסף תרופה" else "ערוך תרופה"

        if (editMedId != -1L) loadExistingData()

        binding.btnCamera.setOnClickListener { checkCameraAndStart() }
        binding.btnPickContact.setOnClickListener { pickContact() }
        binding.btnSave.setOnClickListener { saveMedication() }
    }

    private fun loadExistingData() {
        executor.execute {
            val med = medDAO.getById(editMedId) ?: return@execute
            runOnUiThread {
                binding.editName.setText(med.name)
                binding.editDosage.setText(med.dosage)
                binding.editNotes.setText(med.notes)
                savedPhotoPath = med.photoPath
                contactName = med.contactName
                contactPhone = med.contactPhone
                if (contactName.isNotEmpty())
                    binding.textContact.text = "$contactName ($contactPhone)"
            }
        }
    }

    // ───────── Camera ─────────

    private fun checkCameraAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQ_CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                binding.btnCamera.text = "📸 צלם"
                binding.btnCamera.setOnClickListener { takePhoto() }
            } catch (e: Exception) {
                Toast.makeText(this, "שגיאה בפתיחת מצלמה", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            filesDir,
            "med_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions, executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    savedPhotoPath = photoFile.absolutePath
                    runOnUiThread {
                        binding.textPhotoStatus.text = "✅ תמונה נשמרה"
                        Toast.makeText(this@AddEditMedicationActivity, "תמונה נשמרה!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    runOnUiThread {
                        Toast.makeText(this@AddEditMedicationActivity, "שגיאה בצילום: ${exc.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    // ───────── Contacts ─────────

    private fun pickContact() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        startActivityForResult(intent, REQ_CONTACT)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CONTACT && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val cursor = contentResolver.query(
                    uri,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null, null, null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        contactName = it.getString(0) ?: ""
                        contactPhone = it.getString(1) ?: ""
                        binding.textContact.text = "$contactName ($contactPhone)"
                    }
                }
            }
        }
    }

    // ───────── Save ─────────

    private fun saveMedication() {
        val name = binding.editName.text.toString().trim()
        val dosage = binding.editDosage.text.toString().trim()
        val notes = binding.editNotes.text.toString().trim()
        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute

        if (name.isEmpty() || dosage.isEmpty()) {
            Toast.makeText(this, "שם ומינון הם שדות חובה", Toast.LENGTH_SHORT).show()
            return
        }

        executor.execute {
            val med = Medication(
                id = if (editMedId == -1L) 0 else editMedId,
                name = name,
                dosage = dosage,
                notes = notes,
                photoPath = savedPhotoPath,
                contactName = contactName,
                contactPhone = contactPhone
            )

            val medId: Long
            if (editMedId == -1L) {
                medId = medDAO.insert(med)
            } else {
                medDAO.update(med)
                medId = editMedId
                // מחק לוחות זמנים ישנים
                schDAO.deleteByMedication(medId)
                AlarmScheduler.cancelForMedication(this, medId, listOf())
            }

            // שמור לוח זמנים חדש
            val schedule = Schedule(
                medicationId = medId,
                hour = hour,
                minute = minute,
                daysOfWeek = "1,2,3,4,5,6,7" // כל הימים - אפשר להרחיב
            )
            schDAO.insert(schedule)
            AlarmScheduler.scheduleForMedication(this, medId, name, dosage, listOf(schedule))

            runOnUiThread {
                Toast.makeText(this, "תרופה נשמרה בהצלחה!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CAMERA && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
        dbHelper.close()
    }
}
