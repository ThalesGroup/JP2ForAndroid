package com.gemalto.jp2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class Util {
    private final Context ctx;

    public Util(final Context base) {
        this.ctx = base;
    }

    public void assertBitmapsEqual(Bitmap expected, Bitmap actual) {
        assertBitmapsEqual(null, expected, actual);
    }

    public void assertBitmapsEqual(String message, Bitmap expected, Bitmap actual) {
        assertEquals(message, expected.getWidth(), actual.getWidth());
        assertEquals(message, expected.getHeight(), actual.getHeight());
        int[] pixels1 = new int[expected.getWidth() * expected.getHeight()];
        int[] pixels2 = new int[actual.getWidth() * actual.getHeight()];
        expected.getPixels(pixels1, 0, expected.getWidth(), 0, 0, expected.getWidth(), expected.getHeight());
        actual.getPixels(pixels2, 0, actual.getWidth(), 0, 0, actual.getWidth(), actual.getHeight());
        for (int i = 0; i < pixels1.length; i++) {
            if (pixels1[i] != pixels2[i]) {
                fail((message != null ? message + "; " : "") + String.format("pixel %d different - expected %08X, got %08X", i, pixels1[i], pixels2[i]));
            }
        }
    }

    public void assertBitmapsEqual(int[] expected, int[] actual) {
        assertBitmapsEqual(null, expected, actual);
    }

    public void assertBitmapsEqual(String message, int[] expected, int[] actual) {
        assertEquals((message != null ? message + "; " : "") + "different number of pixels", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != actual[i]) {
                fail((message != null ? message + "; " : "") + String.format("pixel %d different - expected %08X, got %08X", i, expected[i], actual[i]));
            }
        }
    }

    public byte[] loadAssetFile(String name) throws Exception {
        try (InputStream is = ctx.getResources().getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(is.available());
            byte[] buffer = new byte[8192];
            int count;
            while ((count = is.read(buffer)) >= 0) {
                out.write(buffer, 0, count);
            }
            return out.toByteArray();
        }
    }

    public InputStream openAssetStream(String name) throws IOException {
        return ctx.getAssets().open(name);
    }

    public byte[] loadFile(String name) throws Exception {
        try (InputStream is = new FileInputStream(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(is.available());
            byte[] buffer = new byte[8192];
            int count;
            while ((count = is.read(buffer)) >= 0) {
                out.write(buffer, 0, count);
            }
            return out.toByteArray();
        }
    }

    public Bitmap loadAssetBitmap(String name) throws Exception {
        try (InputStream is = ctx.getResources().getAssets().open(name)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            opts.inPremultiplied = false;
            Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
            if (bmp.getConfig() != Bitmap.Config.ARGB_8888) {
                //convert to ARGB_8888 for pixel comparison purposes
                int[] pixels = new int[bmp.getWidth() * bmp.getHeight()];
                bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
                bmp = Bitmap.createBitmap(pixels, bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
            }
            return bmp;
        }
    }

    public int[] loadAssetRawPixels(String name) throws Exception {
        //raw bitmaps are stored by component in RGBA order (i.e.) first all R, then all G, then all B, then all A
        byte[] data = null;
        try (InputStream is = ctx.getResources().getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(is.available());
            byte[] buffer = new byte[8192];
            int count;
            while ((count = is.read(buffer)) >= 0) {
                out.write(buffer, 0, count);
            }
            data = out.toByteArray();
        }

        assertEquals("raw data length not divisible by 4", 0, data.length % 4);
        int length = data.length / 4;
        int[] pixels = new int[length];
        for (int i = 0; i < length; i++) {
            pixels[i] = ((data[i]              & 0xFF) << 16)  //R
                        | ((data[i + length]     & 0xFF) << 8)   //G
                        | ( data[i + length * 2] & 0xFF)         //B
                        | ((data[i + length * 3] & 0xFF) << 24); //A
        }
        return pixels;
    }

    public Bitmap loadAssetRawBitmap(String name, int width, int height) throws Exception {
        int[] pixels = loadAssetRawPixels(name);
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);

    }

    public File createFile(String name, byte[] encoded) throws IOException {
        File outFile = new File(ctx.getFilesDir(), name);
        FileOutputStream out = new FileOutputStream(outFile);
        out.write(encoded);
        out.close();
        return outFile;
    }

    public File createFile(byte[] encoded) throws IOException {
        File outFile = File.createTempFile("testjp2", "tmp", ctx.getFilesDir());
        FileOutputStream out = new FileOutputStream(outFile);
        out.write(encoded);
        out.close();
        return outFile;
    }
}
