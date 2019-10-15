package com.elapse.democamerax;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    static final String KEY_EVENT_ACTION = "key_event_action";
    static final String KEY_EVENT_EXTRA = "key_event_extra";
    private static final long IMMERSIVE_FLAG_TIMEOUT = 500L;
    private FrameLayout container;

    //    int screenWidth,screenHeight;
//    private static final int REQUEST_CODE_PERMISSION = 0x01;
//    private String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
//    TextureView viewFinder;
//
//    private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        container = findViewById(R.id.fragment_container);
//
//        viewFinder = findViewById(R.id.view_finder);
//        DisplayMetrics dm = getResources().getDisplayMetrics();
//
//        screenHeight = dm.heightPixels;
//        screenWidth = dm.widthPixels;
//        Log.e(TAG, "onCreate: "+screenWidth+":"+screenHeight );
//
//        if (allPermissionsGranted()) {
//            viewFinder.post(new Runnable() {
//                @Override
//                public void run() {
//                    startCamera();
//                }
//            });
//        } else {
//            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION);
//        }
//
//        viewFinder.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
//            @Override
//            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
//                updateTransform();
//            }
//        });
//
    }

    @Override
    protected void onResume() {
        super.onResume();
        container.postDelayed(new Runnable() {
            @Override
            public void run() {
                container.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);


            }
        }, IMMERSIVE_FLAG_TIMEOUT);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Intent intent = new Intent(KEY_EVENT_ACTION);
            intent.putExtra(KEY_EVENT_EXTRA,keyCode);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            return true;
        } else
            return super.onKeyDown(keyCode, event);
    }

    public static File getOutputDirectory(Context context){
        Context applicationContext = context.getApplicationContext();
        File mediaDir = context.getExternalMediaDirs()[0];
        File output = null;
        if (mediaDir != null){
            output = new File(mediaDir,context.getString(R.string.app_name));
            if (!output.exists()){
                output.mkdirs();
            }
        }

        if (output != null && output.exists()){
            return output;
        }else {
            return applicationContext.getFilesDir();
        }
    }

    //
//    private void startCamera() {
//        PreviewConfig previewConfig = new PreviewConfig.Builder()
//                .setTargetAspectRatio(new Rational(4, 3))
////                .setTargetResolution(new Size(1280, 960))
////                .setTargetRotation(viewFinder.getDisplay().getRotation())
//                .build();
//
//        // Build the viewfinder use case
//        Preview preview = new Preview(previewConfig);
//
//        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
//            @Override
//            public void onUpdated(Preview.PreviewOutput output) {
//                ViewGroup parent = (ViewGroup) viewFinder.getParent();
//                parent.removeView(viewFinder);
//                parent.addView(viewFinder, 0);
//                viewFinder.setSurfaceTexture(output.getSurfaceTexture());
//                updateTransform();
//            }
//        });
//
//        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
//                .setTargetAspectRatio(new Rational(screenWidth, screenHeight))
//                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
//                .build();
//
//        // Build the image capture use case and attach button click listener
//        final ImageCapture imageCapture = new ImageCapture(imageCaptureConfig);
//        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                File file = new File(getExternalMediaDirs()[0], System.currentTimeMillis() + ".jpg");
//                imageCapture.takePicture(file, new ImageCapture.OnImageSavedListener() {
//                    @Override
//                    public void onImageSaved(@NonNull File file) {
//                        String msg = "Photo capture succeeded: ${file.absolutePath}";
//                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
//                        Log.d("CameraXApp", msg);
//                    }
//
//                    @Override
//                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
//                        String msg = "Photo capture failed: $message";
//                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
//                    }
//                });
//            }
//        });
//
//        CameraX.bindToLifecycle(this, preview, imageCapture);
//    }
//
//    private void updateTransform() {
//        Matrix matrix = new Matrix();
//
//        // Compute the center of the view finder
//        float centerX = viewFinder.getWidth() / 2f;
//        float centerY = viewFinder.getHeight() / 2f;
//
//        // Correct preview output to account for display rotation
//        int rotationDegrees = viewFinder.getDisplay().getRotation();
//        switch (rotationDegrees) {
//            case Surface.ROTATION_0:
//                rotationDegrees = 0;
//                break;
//            case Surface.ROTATION_90:
//                rotationDegrees = 90;
//                break;
//            case Surface.ROTATION_180:
//                rotationDegrees = 180;
//                break;
//            case Surface.ROTATION_270:
//                rotationDegrees = 270;
//                break;
//            default:
//                break;
//        }
//
//        matrix.postRotate(-rotationDegrees, centerX, centerY);
//        // Finally, apply transformations to our TextureView
//        viewFinder.setTransform(matrix);
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == REQUEST_CODE_PERMISSION) {
//            if (allPermissionsGranted()) {
//                viewFinder.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        startCamera();
//                    }
//                });
//            } else {
//                Toast.makeText(MainActivity.this, "permission not granted", Toast.LENGTH_SHORT).show();
//                finish();
//            }
//        }
//    }
//
//    private boolean allPermissionsGranted() {
//        boolean flag = false;
//        for (String permission : permissions) {
//            flag = ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_GRANTED;
//        }
//        return flag;
//    }
}
