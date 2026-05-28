package com.meditrack.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.meditrack.app.R;

import java.io.File;
import java.util.concurrent.Executors;

public class CameraActivity extends BaseActivity {

    public static final String EXTRA_IMAGE_PATH = "IMAGE_PATH";

    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);
        ImageButton btnCapture = findViewById(R.id.btnCapture);
        ImageButton btnFlipCamera = findViewById(R.id.btnFlipCamera);

        btnCapture.setOnClickListener(v -> captureImage());
        btnFlipCamera.setOnClickListener(v -> {
            lensFacing = lensFacing == CameraSelector.LENS_FACING_BACK
                    ? CameraSelector.LENS_FACING_FRONT
                    : CameraSelector.LENS_FACING_BACK;
            bindCameraUseCases();
        });

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindCameraUseCases();
            } catch (Exception e) {
                showToast("שגיאה בפתיחת המצלמה: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            return;
        }

        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, selector, preview, imageCapture);
    }

    private void captureImage() {
        if (imageCapture == null) {
            showToast("המצלמה לא מוכנה");
            return;
        }

        File outputDir = new File(getFilesDir(), "images");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        File outputFile = new File(outputDir, "med_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        imageCapture.takePicture(options,
                Executors.newSingleThreadExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        Intent result = new Intent();
                        result.putExtra(EXTRA_IMAGE_PATH, outputFile.getAbsolutePath());
                        setResult(RESULT_OK, result);
                        runOnUiThread(() -> finish());
                    }

                    @Override
                    public void onError(ImageCaptureException exception) {
                        runOnUiThread(() ->
                                showToast("שגיאה בצילום: " + exception.getMessage()));
                    }
                });
    }
}
