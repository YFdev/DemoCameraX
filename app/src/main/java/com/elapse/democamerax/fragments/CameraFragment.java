package com.elapse.democamerax.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.elapse.democamerax.MainActivity;
import com.elapse.democamerax.R;
import com.elapse.democamerax.util.AutoFitBuilder;
import com.elapse.democamerax.util.ImageUtils;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * author : Kevin.ning
 * e-mail :
 * date   : 2019/10/15 10:57
 * desc   :
 * version: 1.0
 */
public class CameraFragment extends Fragment {
    private static final String TAG = "CameraXBasic";
    private static final String FILENAME = "yyyyMMddHHmmss";//和官方不同，为了便于排序
    private static final String PHOTO_EXTENSION = ".jpg";
    private static final String ACTION_UPDATE_THUMBNAIL = "action_update_thumbnail";
    //    private static final String ACTION_KEY_DOWN = "action_key_down";
    private static final long ANIMATION_SLOW_MILLIS = 500;
    private static final long ANIMATION_FAST_MILLIS = 300;

    /**
     * Helper function used to create a timestamped file
     */
    @SuppressLint("SimpleDateFormat")
    private File createFile(File baseFolder) {
        return new File(baseFolder,
                new SimpleDateFormat(CameraFragment.FILENAME).format(System.currentTimeMillis()) + CameraFragment.PHOTO_EXTENSION);
    }

    private ConstraintLayout container;
    private TextureView viewFinder;
    private File outputDirectory;
    private LocalBroadcastManager broadcastManager;
    private int displayId = -1;
    private CameraX.LensFacing lensFacing = CameraX.LensFacing.BACK;
    private Preview preview;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalyzer;

    // Volume down button receiver
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //处理VOLUME_DOWN拍照逻辑
            if (Objects.equals(intent.getAction(), MainActivity.KEY_EVENT_ACTION)) {
                int keyCode = intent.getIntExtra(MainActivity.KEY_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN);
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    final ImageButton shutter = container.findViewById(R.id.camera_capture_button);
                    shutter.performClick();
                    shutter.setPressed(true);
                    shutter.invalidate();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            shutter.invalidate();
                            shutter.setPressed(false);
                        }
                    }, ANIMATION_SLOW_MILLIS);
                }
            }
            //处理更新缩略图逻辑
            else if (Objects.equals(intent.getAction(), ACTION_UPDATE_THUMBNAIL)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    thumbnail.setForeground(new BitmapDrawable(getResources(), thumbnailBitmap));
                } else {
                    Glide.with(requireContext()).load(thumbnailBitmap).into(thumbnail);
                }
            }
        }
    };

    /**
     * Internal reference of the [DisplayManager]
     */
    private DisplayManager displayManager;
    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {

        }

        @Override
        public void onDisplayRemoved(int displayId) {

        }

        @Override
        public void onDisplayChanged(int displayId) {
            if (displayId == CameraFragment.this.displayId) {
                if (preview != null) {
                    preview.setTargetRotation(Objects.requireNonNull(getView()).getDisplay().getRotation());
                }
                if (imageCapture != null) {
                    imageCapture.setTargetRotation(Objects.requireNonNull(getView()).getDisplay().getRotation());
                }

                if (imageAnalyzer != null) {
                    imageAnalyzer.setTargetRotation(Objects.requireNonNull(getView()).getDisplay().getRotation());
                }
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister the broadcast receivers and listeners
        broadcastManager.unregisterReceiver(mReceiver);
        displayManager.unregisterDisplayListener(displayListener);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private ImageButton thumbnail;
    private Bitmap thumbnailBitmap;

    private void setGalleryThumbnail(final File file) {
        // Reference of the view that holds the gallery thumbnail
        thumbnail = container.findViewById(R.id.photo_view_button);
//        val  = container.findViewById<ImageButton>(R.id.photo_view_button)

        //以下代码在kotlin中使用协程处理，很方便，用Java改写成子线程+广播
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = ImageUtils.decodeBitmap(file);
                thumbnailBitmap = ImageUtils.cropCircularThumbnail(bitmap, 0);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(ACTION_UPDATE_THUMBNAIL));
            }
        }).start();
    }

    /**
     * Define callback that will be triggered after a photo has been taken and saved to disk
     */
    private ImageCapture.OnImageSavedListener imageSavedListener = new ImageCapture.OnImageSavedListener() {
        @Override
        public void onImageSaved(@NonNull File photoFile) {
            Log.d(TAG, "Photo capture succeeded: ${photoFile.absolutePath}");
            // We can only change the foreground Drawable using API level 23+ API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Update the gallery thumbnail with latest picture taken
                setGalleryThumbnail(photoFile);
            }
            // Implicit broadcasts will be ignored for devices running API
            // level >= 24, so if you only target 24+ you can remove this statement
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                requireActivity().sendBroadcast(new Intent(Camera.ACTION_NEW_PICTURE).setData(Uri.fromFile(photoFile)));
            }
            // If the folder selected is an external media directory, this is unnecessary
            // but otherwise other apps will not be able to access our images unless we
            // scan them using [MediaScannerConnection]
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(PHOTO_EXTENSION);
            //通知系统更新相册
            MediaScannerConnection.scanFile(
                    getContext(), new String[]{photoFile.getAbsolutePath()}, new String[]{mimeType}, null);
        }

        @Override
        public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
            Log.e(TAG, "Photo capture failed: $message");
            assert cause != null;
            cause.printStackTrace();
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = (ConstraintLayout) view;
        viewFinder = container.findViewById(R.id.view_finder);
        broadcastManager = LocalBroadcastManager.getInstance(view.getContext());
        IntentFilter filter = new IntentFilter();
        filter.addAction(MainActivity.KEY_EVENT_ACTION);
        filter.addAction(ACTION_UPDATE_THUMBNAIL);
        broadcastManager.registerReceiver(mReceiver, filter);
        // Every time the orientation of device changes, recompute layout
        displayManager = (DisplayManager) viewFinder.getContext()
                .getSystemService(Context.DISPLAY_SERVICE);
        assert displayManager != null;
        displayManager.registerDisplayListener(displayListener, null);

        // Determine the output directory
        outputDirectory = MainActivity.getOutputDirectory(requireContext());

        // Wait for the views to be properly laid out
        viewFinder.post(new Runnable() {
            @Override
            public void run() {
                displayId = viewFinder.getDisplay().getDisplayId();
                // Build UI controls and bind all camera use cases
                updateCameraUi();
                bindCameraUseCases();

                // Keep track of the display in which this view is attached
                // In the background, load latest photo taken (if any) for gallery thumbnail
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        List<File> fileList = Arrays.asList(Objects.requireNonNull(outputDirectory.listFiles()));
                        Collections.sort(fileList, new Comparator<File>() {
                            @Override
                            public int compare(File f1, File f2) {
                                String s1 = f1.getName().substring(0, f1.getName().lastIndexOf("."));
                                String s2 = f1.getName().substring(0, f2.getName().lastIndexOf("."));
                                try {
                                    Long l1 = Long.valueOf(s1);
                                    Long l2 = Long.valueOf(s2);
                                    return (int) (l2 - l1);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return 0;
                                }
                            }
                        });
                        for (File file : fileList) {
                            String name = file.getName();
                            String suffix = name.substring(name.lastIndexOf("."));
                            if ("JPG".equals(suffix)) {
                                setGalleryThumbnail(file);
                                return;
                            }
                        }
                    }
                }).start();
            }
        });
    }

    /**
     * Declare and bind preview, capture and analysis use cases
     * 更新相机参数均需要调用
     */
    private void bindCameraUseCases() {
        // Make sure that there are no other use cases bound to CameraX
        CameraX.unbindAll();
        DisplayMetrics metrics = new DisplayMetrics();
        viewFinder.getDisplay().getRealMetrics(metrics);
        Rational screenAspectRatio = new Rational(metrics.widthPixels, metrics.heightPixels);
        Log.d(TAG, "Metrics: ${metrics.widthPixels} x ${metrics.heightPixels}");

        // Set up the view finder use case to display camera preview
        PreviewConfig.Builder viewFinderConfigBuilder = new PreviewConfig.Builder();
        viewFinderConfigBuilder.setLensFacing(lensFacing);
        // We request aspect ratio but no resolution to let CameraX optimize our use cases
        viewFinderConfigBuilder.setTargetAspectRatio(screenAspectRatio);
        // Set initial target rotation, we will have to call this again if rotation changes
        // during the lifecycle of this use case
        viewFinderConfigBuilder.setTargetRotation(viewFinder.getDisplay().getRotation());
        PreviewConfig config = viewFinderConfigBuilder.build();

        // Use the auto-fit preview builder to automatically handle size and orientation changes
        preview = AutoFitBuilder.build(config, viewFinder);

        // Set up the capture use case to allow users to take photos
        ImageCaptureConfig.Builder builder = new ImageCaptureConfig.Builder();
        builder.setLensFacing(lensFacing);
        builder.setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY);
        // We request aspect ratio but no resolution to match preview config but letting
        // CameraX optimize for whatever specific resolution best fits requested capture mode
        builder.setTargetAspectRatio(screenAspectRatio);
        // Set initial target rotation, we will have to call this again if rotation changes
        // during the lifecycle of this use case
        builder.setTargetRotation(viewFinder.getDisplay().getRotation());
        ImageCaptureConfig captureConfig = builder.build();

        imageCapture = new ImageCapture(captureConfig);
        // Setup image analysis pipeline that computes average pixel luminance in real time
        ImageAnalysisConfig.Builder builder1 = new ImageAnalysisConfig.Builder();
        builder1.setLensFacing(lensFacing);
        // Use a worker thread for image analysis to prevent preview glitches
        HandlerThread analyzerThread = new HandlerThread("LuminosityAnalysis");
        analyzerThread.start();

        builder1.setCallbackHandler(new Handler(analyzerThread.getLooper()));
        // In our analysis, we care more about the latest image than analyzing *every* image
        builder1.setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE);
        // Set initial target rotation, we will have to call this again if rotation changes
        // during the lifecycle of this use case
        builder1.setTargetRotation(viewFinder.getDisplay().getRotation());
        ImageAnalysisConfig analysisConfig = builder1.build();
        imageAnalyzer = new ImageAnalysis(analysisConfig);
        LuminosityAnalyzer analyzer = new LuminosityAnalyzer();
        analyzer.onFrameAnalyzed(new AnalysisCallBack() {
            @Override
            public void onAnalysis(int luma) {
                Log.d(TAG, "Average luminosity:" + luma + "Frames per second.");
            }
        });
        imageAnalyzer.setAnalyzer(analyzer);

        // Apply declared configs to CameraX using the same lifecycle owner
        CameraX.bindToLifecycle(this, preview, imageCapture, imageAnalyzer);
    }

    /**
     * Method used to re-draw the camera UI controls, called every time configuration changes
     */
    @SuppressLint("RestrictedApi")
    private void updateCameraUi() {
        // Remove previous UI if any
        ConstraintLayout ui_container = container.findViewById(R.id.camera_ui_container);
        if (ui_container != null)
            container.removeView(ui_container);
        // Inflate a new view containing all UI for controlling the camera
        ConstraintLayout controls = (ConstraintLayout) View.inflate(requireContext(), R.layout.camera_ui_container, container);

        // Listener for button used to capture photo
        controls.findViewById(R.id.camera_capture_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get a stable reference of the modifiable image capture use case
                if (imageCapture != null) {
                    File photoFile = createFile(outputDirectory);
                    ImageCapture.Metadata metadata = new ImageCapture.Metadata();
                    metadata.isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT;
                    imageCapture.takePicture(photoFile, imageSavedListener, metadata);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        // Display flash animation to indicate that photo was captured
                        container.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                container.setForeground(new ColorDrawable(Color.WHITE));
                                container.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        container.setForeground(null);
                                    }
                                }, ANIMATION_SLOW_MILLIS);
                            }
                        }, ANIMATION_FAST_MILLIS);
                    }
                }
            }
        });

        // Listener for button used to switch cameras
        controls.findViewById(R.id.camera_switch_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CameraX.LensFacing.FRONT == lensFacing) {
                    lensFacing = CameraX.LensFacing.BACK;
                } else {
                    lensFacing = CameraX.LensFacing.FRONT;
                }
                try {
                    // Only bind use cases if we can query a camera with this orientation
                    CameraX.getCameraWithLensFacing(lensFacing);
                    bindCameraUseCases();
                } catch (Exception e) {
                    // Do nothing
                }
            }
        });

        // Listener for button used to view last photo
        controls.findViewById(R.id.photo_view_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //todo
                replaceFragment(new GalleryFragment());
            }
        });

    }

    private void replaceFragment(Fragment fragment) {
        Bundle arguments = new Bundle();
        arguments.putString(GalleryFragment.KEY_ROOT_DIRECTORY, outputDirectory.getAbsolutePath());
        fragment.setArguments(arguments);
        FragmentManager manager = getFragmentManager();
        assert manager != null;
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Our custom image analysis class.
     *
     * <p>All we need to do is override the function `analyze` with our desired operations. Here,
     * we compute the average luminosity of the image by looking at the Y plane of the YUV frame.
     *
     * 亮度滤镜，来自官方
     */
    class LuminosityAnalyzer implements ImageAnalysis.Analyzer {
        private int frameRateWindow = 8;
        private ArrayDeque<Long> frameTimestamps = new ArrayDeque<Long>(5);
        private ArrayList<AnalysisCallBack> listeners = new ArrayList<>();
        private long lastAnalyzedTimestamp = 0L;
        double framesPerSecond = -1.0;

        /**
         * Used to add listeners that will be called with each luma computed
         */
        void onFrameAnalyzed(AnalysisCallBack listener) {
            listeners.add(listener);
        }

        /**
         * Helper extension function used to extract a byte array from an image plane buffer
         */
        private byte[] toByteArray(ByteBuffer buffer) {
            buffer.rewind();// Rewind the buffer to zero
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);  // Copy the buffer into a byte array
            return data;// Return the byte array
        }

        /**
         * Analyzes an image to produce a result.
         *
         * <p>The caller is responsible for ensuring this analysis method can be executed quickly
         * enough to prevent stalls in the image acquisition pipeline. Otherwise, newly available
         * images will not be acquired and analyzed.
         *
         * <p>The image passed to this method becomes invalid after this method returns. The caller
         * should not store external references to this image, as these references will become
         * invalid.
         *
         * @param image image being analyzed VERY IMPORTANT: do not close the image, it will be
         *              automatically closed after this method returns
         * @return the image analysis result
         */

        @Override
        public void analyze(ImageProxy image, int rotationDegrees) {
            if (listeners.isEmpty()) {
                return;
            }
            // Keep track of frames analyzed
            frameTimestamps.push(System.currentTimeMillis());

            // Compute the FPS using a moving average
            while (frameTimestamps.size() >= frameRateWindow)
                frameTimestamps.removeLast();
            framesPerSecond = 1.0 / ((frameTimestamps.peekFirst() - frameTimestamps.peekLast()) / frameTimestamps.size()) * 1000.0;

            // Calculate the average luma no more often than every second
            if (frameTimestamps.getFirst() - lastAnalyzedTimestamp >= TimeUnit.SECONDS.toMillis(1)) {
                // Since format in ImageAnalysis is YUV, image.planes[0] contains the Y
                // (luminance) plane
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                // Extract image data from callback object
                byte[] data = null;
                if (buffer != null) {
                    data = toByteArray(buffer);
                    int sum = 0;
                    for (byte datum : data) {
                        sum += datum & 0xFF;
                    }
                    // Compute average luminance for the image
                    int luma = sum / data.length;
                    // Call all listeners with new value

                    for (AnalysisCallBack callBack : listeners) {
                        callBack.onAnalysis(luma);
                    }
                }
                lastAnalyzedTimestamp = frameTimestamps.getFirst();
            }
        }
    }

    interface AnalysisCallBack {
        void onAnalysis(int luma);
    }
}




