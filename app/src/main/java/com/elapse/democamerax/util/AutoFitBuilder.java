package com.elapse.democamerax.util;

import android.content.Context;
import android.graphics.Matrix;
import android.hardware.display.DisplayManager;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * author : Kevin.ning
 * e-mail :
 * date   : 2019/10/10 16:15
 * desc   :重要辅助类，处理预览尺寸
 * version: 1.0
 */
public class AutoFitBuilder {
    private WeakReference<TextureView> viewFinderRef;

    private AutoFitBuilder(PreviewConfig config, WeakReference<TextureView> viewFinderRef) {
        this.viewFinderRef = viewFinderRef;
        init(config);
    }

    private void init(PreviewConfig config) {
        TextureView viewFinder = viewFinderRef.get();
        if (viewFinder == null) {
            throw new IllegalArgumentException("Invalid reference to view finder used");
        }

        // Initialize the display and rotation from texture view information
        viewFinderDisplay = viewFinder.getDisplay().getDisplayId();
        viewFinderRotation = getDisplaySurfaceRotation(viewFinder.getDisplay());

        // Initialize public use-case with the given config
        useCase = new Preview(config);

        // Every time the view finder is updated, recompute layout
        useCase.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput output) {
                TextureView viewFinder = viewFinderRef.get();
                if (viewFinder == null) {
                    return;
                }
                ViewGroup parent = (ViewGroup) viewFinder.getParent();
                parent.removeView(viewFinder);
                parent.addView(viewFinder, 0);
                viewFinder.setSurfaceTexture(output.getSurfaceTexture());
                bufferRotation = output.getRotationDegrees();
                int rotation = getDisplaySurfaceRotation(viewFinder.getDisplay());
                updateTransform(viewFinder, rotation, output.getTextureSize(), viewFinderDimens);
            }
        });
        viewFinder.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom, int i4, int i5, int i6, int i7) {
                TextureView viewFinder = (TextureView) view;
                Size newViewFinderDimens = new Size(right - left, bottom - top);
                int rotation = getDisplaySurfaceRotation(viewFinder.getDisplay());
                updateTransform(viewFinder, rotation, bufferDimens, newViewFinderDimens);
            }
        });

        // Every time the orientation of device changes, recompute layout
        mDisplayManager = (DisplayManager) viewFinder.getContext().getSystemService(Context.DISPLAY_SERVICE);
        assert mDisplayManager != null;
        mDisplayManager.registerDisplayListener(mDisplayListener, null);

        // Remove the display listeners when the view is detached to avoid
        // holding a reference to the View outside of a Fragment.
        // NOTE: Even though using a weak reference should take care of this,
        // we still try to avoid unnecessary calls to the listener this way.
        viewFinder.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
                mDisplayManager.registerDisplayListener(mDisplayListener, null);
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                mDisplayManager.unregisterDisplayListener(mDisplayListener);
            }
        });
    }

    private Preview useCase;
    private int bufferRotation = 0;
    private int viewFinderRotation = 0;
    private Size bufferDimens = new Size(0, 0);
    private Size viewFinderDimens = new Size(0, 0);
    private int viewFinderDisplay = -1;
    private DisplayManager mDisplayManager;
    private DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int i) {

        }

        @Override
        public void onDisplayRemoved(int i) {

        }

        @Override
        public void onDisplayChanged(int displayId) {
            TextureView viewFinder = viewFinderRef.get();
            if (viewFinder == null) return;
            if (displayId == viewFinderDisplay) {
                Display display = mDisplayManager.getDisplay(displayId);
                int rotation = getDisplaySurfaceRotation(display);
                updateTransform(viewFinder, rotation, bufferDimens, viewFinderDimens);
            }
        }
    };

    private void updateTransform(TextureView textureView, int rotation, Size newBufferDimens,
                                 Size newViewFinderDimens) {
        // This should not happen anyway, but now the linter knows
        if (textureView == null) return;

        if (rotation == viewFinderRotation &&
                Objects.equals(newBufferDimens, bufferDimens) &&
                Objects.equals(newViewFinderDimens, viewFinderDimens)) {
            // Nothing has changed, no need to transform output again
            return;
        }

        // Update internal field with new inputs
        viewFinderRotation = rotation;
        if (newBufferDimens.getWidth() == 0 || newBufferDimens.getHeight() == 0) {
            // Invalid buffer dimens - wait for valid inputs before setting matrix
            return;
        } else {
            // Update internal field with new inputs
            bufferDimens = newBufferDimens;
        }

        if (newViewFinderDimens.getWidth() == 0 || newViewFinderDimens.getHeight() == 0) {
            // Invalid view finder dimens - wait for valid inputs before setting matrix
            return;
        } else {
            // Update internal field with new inputs
            viewFinderDimens = newViewFinderDimens;
        }

        Matrix matrix = new Matrix();
        // Compute the center of the view finder
        float centerX = viewFinderDimens.getWidth() / 2f;
        float centerY = viewFinderDimens.getHeight() / 2f;

        // Correct preview output to account for display rotation
        matrix.postRotate(-viewFinderRotation, centerX, centerY);
        // Buffers are rotated relative to the device's 'natural' orientation: swap width and height
        float bufferRatio = bufferDimens.getHeight() *1f / bufferDimens.getWidth();

        int scaledWidth;
        int scaledHeight;
        // Match longest sides together -- i.e. apply center-crop transformation
        if (viewFinderDimens.getWidth() > viewFinderDimens.getHeight()) {
            scaledHeight = viewFinderDimens.getWidth();
            scaledWidth = Math.round(viewFinderDimens.getWidth() * bufferRatio);
        } else {
            scaledHeight = viewFinderDimens.getHeight();
            scaledWidth = Math.round(viewFinderDimens.getHeight() * bufferRatio);
        }

        // Compute the relative scale value
        float xScale = scaledWidth * 1f/ viewFinderDimens.getWidth();
        float yScale = scaledHeight * 1f/ viewFinderDimens.getHeight();

        // Scale input buffers to fill the view finder
        matrix.preScale(xScale, yScale, centerX, centerY);

        // Finally, apply transformations to our TextureView
        textureView.setTransform(matrix);
    }

    private static int getDisplaySurfaceRotation(Display display) {
        int rotation = 0;
        if (display != null) {
            rotation = display.getRotation();
        }
        if (rotation != Surface.ROTATION_0) {
            if (rotation == Surface.ROTATION_90) rotation = 90;
            else if (rotation == Surface.ROTATION_180) rotation = 180;
            else if (rotation == Surface.ROTATION_270) rotation = 270;
            else rotation = 0;
        }
        return rotation;
    }

    public static Preview build(PreviewConfig config, TextureView viewFinder) {
        return new AutoFitBuilder(config, new WeakReference<TextureView>(viewFinder)).useCase;
    }
}
