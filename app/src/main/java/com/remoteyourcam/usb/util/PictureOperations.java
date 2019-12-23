package com.remoteyourcam.usb.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class PictureOperations {
    private static Bitmap borderBitmap;

    public static Bitmap applyBorder(Bitmap picture) {
        return applyBorder(picture, 1);
    }

    public static Bitmap applyBorder(Bitmap picture, float scaleBy) {
        if (borderBitmap == null) {
            return picture;
        }
        Bitmap result = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(), picture.getConfig());
        Canvas canvas = new Canvas(result);
        if (scaleBy < 1) {
            picture = Bitmap.createScaledBitmap(picture, Math.round(picture.getWidth() * scaleBy), Math.round(picture.getHeight() * scaleBy), false);
        }
        canvas.drawBitmap(picture, Math.round((result.getWidth() - picture.getWidth()) / 2), Math.round((result.getHeight() - picture.getHeight()) / 2), null);
        canvas.drawBitmap(borderBitmap, 0f, 0f, null);
        return result;
    }

    public static void setBorderBitmap(Bitmap borderBitmap) {
        PictureOperations.borderBitmap = borderBitmap;
    }
}
