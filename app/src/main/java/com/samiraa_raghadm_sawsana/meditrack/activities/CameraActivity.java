package com.samiraa_raghadm_sawsana.meditrack.activities;

import android.os.Bundle;
import android.view.TextureView;
import android.widget.ImageButton;

import com.samiraa_raghadm_sawsana.meditrack.R;

/**
 * In-app camera screen for capturing a medication photo.
 *
 * NOTE: CameraX has been removed from the project. This screen must be
 * rebuilt using the Camera2 API in Java (task 3.2 in the corrected plan):
 *   - CameraManager to list cameras and open the back CameraDevice
 *   - TextureView (R.id.previewView) bound via SurfaceTexture for live preview
 *   - CameraCaptureSession for the preview + capture requests
 *   - ImageReader to receive the JPEG, saved to filesDir/images/ on a
 *     background thread (ExecutorService)
 *   - Close CameraDevice + CaptureSession in onPause()/onDestroy()
 *
 * This is a temporary compiling stub so the rest of the app still builds
 * while the Camera2 implementation is in progress.
 */
public class CameraActivity extends BaseActivity {

    public static final String EXTRA_IMAGE_PATH = "IMAGE_PATH";

    private TextureView previewView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);

        ImageButton btnClose = findViewById(R.id.btnClose);
        ImageButton btnCapture = findViewById(R.id.btnCapture);
        ImageButton btnFlipCamera = findViewById(R.id.btnFlipCamera);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        // TODO (task 3.2): open Camera2 device and start preview on previewView.

        if (btnCapture != null) {
            btnCapture.setOnClickListener(v ->
                    // TODO (task 3.3): capture via ImageReader, save JPEG, return path.
                    showToast(getString(R.string.error_camera_not_ready)));
        }

        if (btnFlipCamera != null) {
            btnFlipCamera.setOnClickListener(v ->
                    // TODO (task 3.2): switch between back/front CameraDevice.
                    showToast(getString(R.string.error_camera_not_ready)));
        }
    }
}
