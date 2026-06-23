package com.samiraa_raghadm_sawsana.meditrack.activities;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.database.DatabaseHelper;
import com.samiraa_raghadm_sawsana.meditrack.database.MedicationDAO;
import com.samiraa_raghadm_sawsana.meditrack.helpers.PermissionManager;
import com.samiraa_raghadm_sawsana.meditrack.models.Medication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends BaseActivity {

    public static final String EXTRA_IMAGE_PATH = "IMAGE_PATH";
    public static final String EXTRA_MEDICATION_ID = "MEDICATION_ID";
    private static final int THUMBNAIL_MAX_DIMENSION = 512;

    private static final SparseIntArray JPEG_ORIENTATIONS = new SparseIntArray();

    static {
        JPEG_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        JPEG_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        JPEG_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        JPEG_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private TextureView previewView;
    private ImageView lastPhotoView;
    private ImageButton flipCameraButton;
    private ImageButton flashButton;

    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ExecutorService cameraExecutor;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewRequestBuilder;
    private ImageReader imageReader;

    private String activeCameraId;
    private String backCameraId;
    private String frontCameraId;
    private boolean useFrontCamera;
    private boolean isFlashEnabled;
    private boolean isCapturingImage;
    private boolean isClosingCamera;
    private int sensorOrientation;
    private int medicationId;

    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                    ensureCameraReady();
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                    configureTransform(width, height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                }
            };

    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            if (isClosingCamera || !camera.getId().equals(activeCameraId)) {
                camera.close();
                return;
            }
            cameraDevice = camera;
            startPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            if (cameraDevice == camera) {
                cameraDevice = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            if (cameraDevice == camera) {
                cameraDevice = null;
            }
            runOnUiThread(() -> showToast(getString(R.string.error_camera_open,
                    getString(R.string.error_camera_not_ready))));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        medicationId = getIntent().getIntExtra(EXTRA_MEDICATION_ID, 0);
        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraManager = getSystemService(CameraManager.class);

        previewView = findViewById(R.id.previewView);
        lastPhotoView = findViewById(R.id.ivLastPhoto);
        flipCameraButton = findViewById(R.id.btnFlipCamera);
        flashButton = findViewById(R.id.btnFlash);

        registerLaunchers();
        bindUi();
        discoverCameraIds();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (previewView.isAvailable()) {
            ensureCameraReady();
        } else {
            previewView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCameraResources();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        closeCameraResources();
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

    private void discoverCameraIds() {
        if (cameraManager == null) {
            showToast(getString(R.string.error_camera_not_ready));
            finish();
            return;
        }

        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics =
                        cameraManager.getCameraCharacteristics(cameraId);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing == null) {
                    continue;
                }
                if (lensFacing == CameraCharacteristics.LENS_FACING_BACK && backCameraId == null) {
                    backCameraId = cameraId;
                } else if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT
                    && frontCameraId == null) {
                    frontCameraId = cameraId;
                }
            }
            activeCameraId = backCameraId != null ? backCameraId : frontCameraId;
            if (activeCameraId == null) {
                showToast(getString(R.string.error_camera_not_ready));
                finish();
                return;
            }
            flipCameraButton.setEnabled(backCameraId != null && frontCameraId != null);
            flashButton.setEnabled(hasFlashUnit(activeCameraId));
        } catch (CameraAccessException e) {
            showToast(getString(R.string.error_camera_open, e.getMessage()));
            finish();
        }
    }

    private void ensureCameraReady() {
        if (!PermissionManager.isGranted(this, Manifest.permission.CAMERA)) {
            requestCameraPermission();
            return;
        }
        if (activeCameraId == null) {
            showToast(getString(R.string.error_camera_not_ready));
            finish();
            return;
        }
        if (!previewView.isAvailable() || cameraDevice != null) {
            return;
        }
        openCamera(activeCameraId);
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

    private void openCamera(String cameraId) {
        try {
            closeCameraResources();
            isClosingCamera = false;

            CameraCharacteristics characteristics =
                    cameraManager.getCameraCharacteristics(cameraId);
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) != null
                    ? characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) : 0;
            StreamConfigurationMap configMap =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (configMap == null) {
                showToast(getString(R.string.error_camera_not_ready));
                finish();
                return;
            }

            Size jpegSize = chooseLargestSize(configMap.getOutputSizes(ImageFormat.JPEG));
            if (jpegSize == null) {
                jpegSize = new Size(1280, 720);
            }
            imageReader = ImageReader.newInstance(
                    jpegSize.getWidth(),
                    jpegSize.getHeight(),
                    ImageFormat.JPEG,
                    2);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = reader.acquireNextImage();
                if (image == null) {
                    return;
                }
                saveCapturedImage(image);
            }, null);

            activeCameraId = cameraId;
            flashButton.setEnabled(hasFlashUnit(cameraId));
            isFlashEnabled = false;
            if (PermissionManager.isGranted(this, Manifest.permission.CAMERA)) {
                cameraManager.openCamera(cameraId, cameraStateCallback, null);
            }
        } catch (CameraAccessException e) {
            showToast(getString(R.string.error_camera_open, e.getMessage()));
            finish();
        } catch (SecurityException e) {
            showToast(getString(R.string.error_camera_permission));
            finish();
        }
    }

    private void startPreviewSession() {
        if (cameraDevice == null || !previewView.isAvailable() || imageReader == null) {
            return;
        }

        try {
            SurfaceTexture texture = previewView.getSurfaceTexture();
            if (texture == null) {
                return;
            }

            Size previewSize = choosePreviewSize();
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(texture);

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(previewSurface);
            applyFlashMode(previewRequestBuilder);

            List<Surface> surfaces = new ArrayList<>(Arrays.asList(previewSurface, imageReader.getSurface()));
            cameraDevice.createCaptureSession(surfaces,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (cameraDevice == null || session.getDevice() != cameraDevice) {
                                session.close();
                                return;
                            }
                            captureSession = session;
                            try {
                                session.setRepeatingRequest(previewRequestBuilder.build(), null, null);
                                configureTransform(previewView.getWidth(), previewView.getHeight());
                            } catch (CameraAccessException e) {
                                showToast(getString(R.string.error_camera, e.getMessage()));
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            showToast(getString(R.string.error_camera_not_ready));
                        }
                    }, null);
        } catch (CameraAccessException e) {
            showToast(getString(R.string.error_camera, e.getMessage()));
        }
    }

    private void captureStillImage() {
        if (cameraDevice == null || captureSession == null || imageReader == null) {
            showToast(getString(R.string.error_camera_not_ready));
            return;
        }
        if (isCapturingImage) {
            return;
        }

        try {
            isCapturingImage = true;
            CaptureRequest.Builder captureBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            applyFlashMode(captureBuilder);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation());

            captureSession.capture(captureBuilder.build(),
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                       @NonNull CaptureRequest request,
                                                       @NonNull TotalCaptureResult result) {
                            restartPreview();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            isCapturingImage = false;
            showToast(getString(R.string.error_camera, e.getMessage()));
        }
    }

    private void restartPreview() {
        if (captureSession == null || previewRequestBuilder == null) {
            return;
        }
        try {
            captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            showToast(getString(R.string.error_camera, e.getMessage()));
        }
    }

    private void saveCapturedImage(Image image) {
        if (cameraExecutor == null || cameraExecutor.isShutdown()) {
            image.close();
            return;
        }

        cameraExecutor.execute(() -> {
            FileOutputStream outputStream = null;
            File savedFile = null;
            try {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);

                File imageDir = new File(getFilesDir(), "images");
                if (!imageDir.exists() && !imageDir.mkdirs()) {
                    throw new IOException("Failed to create image directory");
                }

                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                        Locale.getDefault()).format(new Date());
                String fileName = "med_" + (medicationId > 0 ? medicationId : "new")
                        + "_" + timestamp + ".jpg";
                savedFile = new File(imageDir, fileName);
                outputStream = new FileOutputStream(savedFile);
                outputStream.write(bytes);
                outputStream.flush();

                if (medicationId > 0) {
                    MedicationDAO dao = new MedicationDAO(DatabaseHelper.getInstance(this));
                    Medication medication = dao.getMedicationById(medicationId);
                    if (medication != null) {
                        medication.setImagePath(savedFile.getAbsolutePath());
                        dao.updateMedication(medication);
                    }
                }

                Bitmap thumbnail = decodeCaptureThumbnail(savedFile);
                File finalSavedFile = savedFile;
                Bitmap finalThumbnail = thumbnail;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    isCapturingImage = false;
                    showLastCapture(finalThumbnail);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_IMAGE_PATH, finalSavedFile.getAbsolutePath());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                });
            } catch (IOException e) {
                if (savedFile != null && savedFile.exists()) {
                    // Only delete the incomplete file written by this capture attempt.
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
            } finally {
                image.close();
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        });
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
        if (backCameraId == null || frontCameraId == null) {
            showToast(getString(R.string.error_camera_not_ready));
            return;
        }
        useFrontCamera = !useFrontCamera;
        String nextCameraId = useFrontCamera ? frontCameraId : backCameraId;
        openCamera(nextCameraId);
    }

    private void toggleFlash() {
        if (!hasFlashUnit(activeCameraId)) {
            showToast(getString(R.string.error_camera_not_ready));
            return;
        }
        isFlashEnabled = !isFlashEnabled;
        restartPreviewWithFlashState();
    }

    private void restartPreviewWithFlashState() {
        if (previewRequestBuilder == null || captureSession == null) {
            return;
        }
        try {
            applyFlashMode(previewRequestBuilder);
            captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            showToast(getString(R.string.error_camera, e.getMessage()));
        }
    }

    private void applyFlashMode(CaptureRequest.Builder builder) {
        if (builder == null) {
            return;
        }
        builder.set(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        if (hasFlashUnit(activeCameraId) && isFlashEnabled) {
            builder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
        } else {
            builder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON);
            builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
        }
    }

    private int getJpegOrientation() {
        int deviceRotation = getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = JPEG_ORIENTATIONS.get(deviceRotation);
        return (rotationCompensation + sensorOrientation + 270) % 360;
    }

    private Size choosePreviewSize() {
        try {
            CameraCharacteristics characteristics =
                    cameraManager.getCameraCharacteristics(activeCameraId);
            StreamConfigurationMap configMap =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (configMap == null) {
                return new Size(1280, 720);
            }

            Size[] sizes = configMap.getOutputSizes(SurfaceTexture.class);
            if (sizes == null || sizes.length == 0) {
                return new Size(1280, 720);
            }

            Size smallestBigEnough = null;
            Size largestAvailable = sizes[0];
            int viewWidth = Math.max(previewView.getWidth(), 1);
            int viewHeight = Math.max(previewView.getHeight(), 1);
            for (Size size : sizes) {
                if ((long) size.getWidth() * size.getHeight()
                        > (long) largestAvailable.getWidth() * largestAvailable.getHeight()) {
                    largestAvailable = size;
                }
                if (size.getWidth() >= viewWidth && size.getHeight() >= viewHeight) {
                    if (smallestBigEnough == null
                            || (long) size.getWidth() * size.getHeight()
                            < (long) smallestBigEnough.getWidth() * smallestBigEnough.getHeight()) {
                        smallestBigEnough = size;
                    }
                }
            }
            return smallestBigEnough != null ? smallestBigEnough : largestAvailable;
        } catch (CameraAccessException e) {
            return new Size(1280, 720);
        }
    }

    private Size chooseLargestSize(Size[] sizes) {
        if (sizes == null || sizes.length == 0) {
            return null;
        }
        return Arrays.stream(sizes)
                .max(Comparator.comparingLong(size -> (long) size.getWidth() * size.getHeight()))
                .orElse(sizes[0]);
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (previewView == null || viewWidth == 0 || viewHeight == 0) {
            return;
        }
        Matrix matrix = new Matrix();
        float centerX = viewWidth / 2f;
        float centerY = viewHeight / 2f;
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        previewView.setTransform(matrix);
    }

    private boolean hasFlashUnit(String cameraId) {
        if (cameraId == null || cameraManager == null) {
            return false;
        }
        try {
            CameraCharacteristics characteristics =
                    cameraManager.getCameraCharacteristics(cameraId);
            Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            return Boolean.TRUE.equals(hasFlash);
        } catch (CameraAccessException e) {
            return false;
        }
    }

    private void closeCameraResources() {
        isClosingCamera = true;
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.setOnImageAvailableListener(null, null);
            imageReader.close();
            imageReader = null;
        }
        previewRequestBuilder = null;
        isCapturingImage = false;
    }
}
