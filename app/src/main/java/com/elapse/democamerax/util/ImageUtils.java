package com.elapse.democamerax.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.ThumbnailUtils;
import androidx.camera.core.ImageProxy;
import androidx.exifinterface.media.ExifInterface;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * author : Kevin.ning
 * e-mail :
 * date   : 2019/10/14 19:22
 * desc   :
 * version: 1.0
 */
public class ImageUtils {

    /**
     * Helper function used to convert an EXIF orientation enum into a transformation matrix
     * that can be applied to a bitmap.
     *
     * @param orientation - One of the constants from [ExifInterface]
     */
    private static Matrix decodeExifOrientation(int orientation) {
        Matrix matrix = new Matrix();
        // Apply transformation corresponding to declared EXIF orientation
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            matrix.postRotate(90F);
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
            matrix.postRotate(180F);
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            matrix.postRotate(270F);
        } else if (orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL) {
            matrix.postScale(-1F, 1F);
        } else if (orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL) {
            matrix.postScale(1F, -1F);
        } else if (orientation == ExifInterface.ORIENTATION_TRANSPOSE) {
            matrix.postScale(-1F, 1F);
            matrix.postRotate(270F);
        } else if (orientation == ExifInterface.ORIENTATION_TRANSVERSE) {
            matrix.postScale(-1F, 1F);
            matrix.postRotate(90F);
        } else if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {

        } else {
            throw new IllegalArgumentException("Invalid orientation: $orientation");
        }
        // Return the resulting matrix
        return matrix;
    }

    /**
     * Decode a bitmap from a file and apply the transformations described in its EXIF data
     *
     * @param file - The image file to be read using [BitmapFactory.decodeFile]
     */

    public static Bitmap decodeBitmap(File file) {
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(file.getAbsolutePath());
            Matrix matrix = decodeExifOrientation(exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90));
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            return Bitmap.createBitmap(
                    BitmapFactory.decodeFile(file.getAbsolutePath()),
                    0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    static Bitmap decodeBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * This function cuts out a circular thumbnail from the provided bitmap. This is done by
     * first scaling the image down to a square with width of [diameter], and then marking all
     * pixels outside of the inner circle as transparent.
     *
     * @param bitmap   - The [Bitmap] to be taken a thumbnail of
     * @param diameter - Size in pixels for the diameter of the resulting circle
     */
    public static Bitmap cropCircularThumbnail(Bitmap bitmap, int diameter) {
        // Extract a much smaller bitmap to serve as thumbnail
        if (diameter == 0){
            diameter = 128;
        }
        Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bitmap, diameter, diameter);

        // Create an additional bitmap of same size as thumbnail to carve a circle out of
        Bitmap circular = Bitmap.createBitmap(
                diameter, diameter, Bitmap.Config.ARGB_8888);
        // Paint will be used as a mask to cut out the circle
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        Canvas canvas = new Canvas(circular);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(diameter / 2F, diameter / 2F, diameter / 2F - 8, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        Rect rect = new Rect(0, 0, diameter, diameter);
        canvas.drawBitmap(thumbnail, rect, rect, paint);
        return circular;
    }

}
