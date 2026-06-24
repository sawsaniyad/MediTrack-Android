package com.samiraa_raghadm_sawsana.meditrack.activities;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.database.DatabaseHelper;
import com.samiraa_raghadm_sawsana.meditrack.database.MedicationDAO;
import com.samiraa_raghadm_sawsana.meditrack.helpers.PermissionManager;
import com.samiraa_raghadm_sawsana.meditrack.models.Medication;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends BaseActivity {

    public static final String EXTRA_IMAGE_PATH = "IMAGE_PATH";
    public static final String EXTRA_MEDICATION_ID = "MEDICATION_ID";
    private static final int THUMBNAIL_MAX_DIMENSION = 512;

    private PreviewView previewView;
    private ImageView lastPhotoView;
    private ImageButton flipCameraButton;
    private ImageButton flashButton;

    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private Camera camera;

    private boolean useFrontCamera;
    private boolean isFlashEnabled;
    private boolean isCapturingImage;
    private int medicationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        medicationId = getIntent().getIntExtra(EXTRA_MEDICATION_ID, 0);
        cameraExecutor = Executors.newSingleThreadExecutor();

        previewView = findViewById(R.id.previewView);
        lastPhotoView = findViewById(R.id.ivLastPhoto);
        flipCameraButton = findViewById(R.id.btnFlipCamera);
        flashButton = findViewById(R.id.btnFlash);

        registerLaunchers();
        bindUi();
        initializeCameraProvider();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensureCameraReady();
    }

    @Override
    protected void onPause() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            camera = null;
            imageCapture = null;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (cameraExecutor != null) {
            cameraExecutor.shutdownNow();
            cameraExecutor = null;
        }
        super.onDestroy();
    }

    private void registerLaunchers() {
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        ensureCameraReady();
                    } else if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.error_camera_denied_title)
                                .setMessage(R.string.error_camera_denied_message)
                                .setPositiveButton(R.string.open_settings,
                                        (dialog, which) -> PermissionManager.openAppSettings(this))
                                .setNegativeButton(R.string.error_camera_continue_without,
                                        (dialog, which) -> finish())
                                .show();
                    } else {
                        showToast(getString(R.string.error_camera_permission));
                        finish();
                    }
                });
    }

    private void bindUi() {
        ImageButton closeButton = findViewById(R.id.btnClose);
        ImageButton captureButton = findViewById(R.id.btnCapture);

        closeButton.setOnClickListener(v -> finish());
        captureButton.setOnClickListener(v -> captureStillImage());
        flipCameraButton.setOnClickListener(v -> switchCamera());
        flashButton.setOnClickListener(v -> toggleFlash());
    }

    private void initializeCameraProvider() {
        ListenableFuture<ProcessCameraProvider> providerFuture = ProcessCameraProvider.getInstance(this);
        providerFuture.addListener(() -> {
            try {
                cameraProvider = providerFuture.get();
                updateLensAvailability();
                ensureCameraReady();
            } catch (ExecutionException | InterruptedException e) {
                showToast(getString(R.string.error_camera_open, e.getMessage()));
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void ensureCameraReady() {
        if (!PermissionManager.isGranted(this, Manifest.permission.CAMERA)) {
            requestCameraPermission();
            return;
        }
        if (cameraProvider == null) {
            return;
        }
        bindCameraUseCases();
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.perm_camera_rationale_title)
                    .setMessage(R.string.perm_camera_rationale_message)
                    .setPositiveButton(R.string.perm_notif_allow,
                            (dialog, which) -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA))
                    .setNegativeButton(R.string.cancel, (dialog, which) -> finish())
                    .show();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void bindCameraUseCases() {
        try {
            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(useFrontCamera
                            ? CameraSelector.LENS_FACING_FRONT
                            : CameraSelector.LENS_FACING_BACK)
                    .build();

            if (!cameraProvider.hasCamera(cameraSelector)) {
                CameraSelector fallbackSelector = new CameraSelector.Builder()
                        .requireLensFacing(useFrontCamera
                                ? CameraSelector.LENS_FACING_BACK
                                : CameraSelector.LENS_FACING_FRONT)
                        .build();
                if (!cameraProvider.hasCamera(fallbackSelector)) {
                    showToast(getString(R.string.error_camera_not_ready));
                    finish();
                    return;
                }
                useFrontCamera = !useFrontCamera;
                cameraSelector = fallbackSelector;
            }

            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(isFlashEnabled
                            ? ImageCapture.FLASH_MODE_ON
                            : ImageCapture.FLASH_MODE_OFF)
                    .build();

            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            updateLensAvailability();
            updateFlashAvailability();
        } catch (Exception e) {
            showToast(getString(R.string.error_camera_open, e.getMessage()));
        }
    }

    private void captureStillImage() {
        if (imageCapture == null) {
            showToast(getString(R.string.error_camera_not_ready));
            return;
        }
        if (isCapturingImage) {
            return;
        }

        File savedFile;
        try {
            savedFile = createOutputFile();
        } catch (IOException e) {
            showToast(getString(R.string.error_camera, e.getMessage()));
            return;
        }

        isCapturingImage = true;
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(savedFile).build();
        imageCapture.takePicture(outputOptions, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        persistCapturedImage(savedFile);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        if (savedFile.exists()) {
                            //noinspection ResultOfMethodCallIgnored
                            savedFile.delete();
                        }
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }
                            isCapturingImage = false;
                            showToast(getString(R.string.error_camera, exception.getMessage()));
                        });
                    }
                });
    }

    private File createOutputFile() throws IOException {
        File imageDir = new File(getFilesDir(), "images");
        if (!imageDir.exists() && !imageDir.mkdirs()) {
            throw new IOException("Failed to create image directory");
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(System.currentTimeMillis());
        String fileName = "med_" + (medicationId > 0 ? medicationId : "new")
                + "_" + timestamp + ".jpg";
        return new File(imageDir, fileName);
    }

    private void persistCapturedImage(File savedFile) {
        try {
            if (medicationId > 0) {
                MedicationDAO dao = new MedicationDAO(DatabaseHelper.getInstance(this));
                Medication medication = dao.getMedicationById(medicationId);
                if (medication != null) {
                    medication.setImagePath(savedFile.getAbsolutePath());
                    dao.updateMedication(medication);
                }
            }

            Bitmap thumbnail = decodeCaptureThumbnail(savedFile);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                isCapturingImage = false;
                showLastCapture(thumbnail);
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_IMAGE_PATH, savedFile.getAbsolutePath());
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        } catch (Exception e) {
            if (savedFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                savedFile.delete();
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                isCapturingImage = false;
                showToast(getString(R.string.error_camera, e.getMessage()));
            });
        }
    }

    private void showLastCapture(Bitmap bitmap) {
        if (bitmap != null) {
            lastPhotoView.setImageBitmap(bitmap);
        }
    }

    private Bitmap decodeCaptureThumbnail(File imageFile) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bounds);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(
                bounds, THUMBNAIL_MAX_DIMENSION, THUMBNAIL_MAX_DIMENSION);
        return BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options,
                                      int reqWidth,
                                      int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        while (height / inSampleSize > reqHeight || width / inSampleSize > reqWidth) {
            inSampleSize *= 2;
        }

        return Math.max(inSampleSize, 1);
    }

    private void switchCamera() {
        if (cameraProvider == null || !flipCameraButton.isEnabled()) {
            showToast(getString(R.string.error_camera_not_ready));
            return;
        }
        useFrontCamera = !useFrontCamera;
        isFlashEnabled = false;
        bindCameraUseCases();
    }

    private void toggleFlash() {
        if (camera == null || camera.getCameraInfo() == null || !camera.getCameraInfo().hasFlashUnit()) {
            showToast(getString(R.string.error_camera_not_ready));
            return;
        }
        isFlashEnabled = !isFlashEnabled;
        if (imageCapture != null) {
            imageCapture.setFlashMode(isFlashEnabled
                    ? ImageCapture.FLASH_MODE_ON
                    : ImageCapture.FLASH_MODE_OFF);
        }
        camera.getCameraControl().enableTorch(isFlashEnabled);
        updateFlashAvailability();
    }

    private void updateLensAvailability() {
        if (cameraProvider == null) {
            flipCameraButton.setEnabled(false);
            return;
        }
        boolean hasBackCamera = hasCameraFacing(CameraSelector.LENS_FACING_BACK);
        boolean hasFrontCamera = hasCameraFacing(CameraSelector.LENS_FACING_FRONT);
        flipCameraButton.setEnabled(hasBackCamera && hasFrontCamera);
    }

    private boolean hasCameraFacing(int lensFacing) {
        if (cameraProvider == null) {
            return false;
        }
        try {
            return cameraProvider.hasCamera(
                    new CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build());
        } catch (CameraInfoUnavailableException e) {
            return false;
        }
    }
    private void updateFlashAvailability() {
        boolean hasFlash = camera != null
                && camera.getCameraInfo() != null
                && camera.getCameraInfo().hasFlashUnit();
        flashButton.setEnabled(hasFlash);
        if (!hasFlash) {
            isFlashEnabled = false;
        }
    }
}

