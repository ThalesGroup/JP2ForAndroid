package com.gemalto.jp2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class TestJp2Decoder {
    // Context of the app under test.
    private Context ctx;
    private Util util;

    @Before
    public void init() {
        ctx = ApplicationProvider.getApplicationContext();
        util = new Util(ctx);
    }

    /*
     * Test that the JP2Decoder.isJPEG2000() method works properly for both valid and invalid data.
     */
    @Test
    public void testIsJPEG2000() throws Exception {
        byte[] data;

        data = util.loadAssetFile("lena.jp2");
        assertTrue("jp2 file not detected as jpeg 2000", JP2Decoder.isJPEG2000(data));
        data = util.loadAssetFile("lena.j2k");
        assertTrue("j2k file not detected as jpeg 2000", JP2Decoder.isJPEG2000(data));
        data = util.loadAssetFile("oj-ht-byte.jph");
        assertTrue("jph file not detected as jpeg 2000", JP2Decoder.isJPEG2000(data));
        data = util.loadAssetFile("butterfly.jhc");
        assertTrue("jhc file not detected as jpeg 2000", JP2Decoder.isJPEG2000(data));
        data = util.loadAssetFile("lena.png");
        assertFalse("png file detected as jpeg 2000", JP2Decoder.isJPEG2000(data));
        data = null;
        assertFalse("null data detected as jpeg 2000", JP2Decoder.isJPEG2000(data));
        data = new byte[0];
        assertFalse("empty data detected as jpeg 2000", JP2Decoder.isJPEG2000(data));
        data = new byte[1];
        assertFalse("short invalid data detected as jpeg 2000", JP2Decoder.isJPEG2000(data));
    }

    /*
     * Decode a JP2 image with all RGB colors - compare with original in PNG
     */
    @Test
    public void testDecodeAllColorsLossless() throws Exception {
        byte encoded[] = util.loadAssetFile("fullrgb.jp2");
        Bitmap bmpExpected = util.loadAssetBitmap("fullrgb.png");
        Bitmap bmpDecoded = new JP2Decoder(encoded).decode();
        assertNotNull(bmpDecoded);

        util.assertBitmapsEqual(bmpExpected, bmpDecoded);
    }

    /*
      Decode a normal image, a greyscale image, a tiny image, in JP2 and J2K format, compare them with the expected results.
     */
    @Test
    public void testDecodeSimple() throws Exception {
        String[] jp2Files = new String[] {"lena.jp2", "lena.j2k", "1x1.jp2", "1x1.j2k", "lena-grey.jp2"};
        String[] expectedFiles = new String[] {"lena.png", "lena.png", "1x1.png", "1x1.png", "lena-grey.png"};


        for (int i = 0; i < jp2Files.length; i++) {
            Bitmap expected = util.loadAssetBitmap(expectedFiles[i]);

            //test decode from file
            File outFile = util.createFile(util.loadAssetFile(jp2Files[i]));

            Bitmap decoded = new JP2Decoder(outFile.getPath()).decode();
            assertFalse(decoded.hasAlpha());

            util.assertBitmapsEqual(expected, decoded);
            outFile.delete();

            //test decode from stream
            try (InputStream in = util.openAssetStream(jp2Files[i])) {
                decoded = new JP2Decoder(in).decode();
                util.assertBitmapsEqual(expected, decoded);
            }

            //test decode from byte array
            byte[] data = util.loadAssetFile(jp2Files[i]);
            decoded = new JP2Decoder(data).decode();
            util.assertBitmapsEqual(expected, decoded);
        }
    }

    /*
      Decode a normal image with specified decoding area and check the results.
     */
    @Test
    public void testDecodingAreaNormal() throws Exception {
        String[] jp2Files = new String[]{"lena.jp2", "lena.j2k"}; //dimensions 512x512
        String[] expectedFiles = new String[]{"lena.png", "lena.png"};

        //test successful decode
        for (int i = 0; i < jp2Files.length; i++) {
            for (Rect decodingArea : new Rect[]{
                    null,
                    new Rect(0, 0, 0, 0),
                    new Rect(0, 0, 150, 500),
                    new Rect(51, 52, 53, 54),
                    new Rect(0, 0, 512, 512),
            }) {
                Bitmap expected = util.loadAssetBitmap(expectedFiles[i], decodingArea);

                //test decode from file
                File outFile = util.createFile(util.loadAssetFile(jp2Files[i]));

                Bitmap decoded = new JP2Decoder(outFile.getPath()).setDecodingArea(decodingArea).decode();
                assertFalse(decoded.hasAlpha());

                util.assertBitmapsEqual(expected, decoded);
                outFile.delete();

                //test decode from stream
                try (InputStream in = util.openAssetStream(jp2Files[i])) {
                    decoded = new JP2Decoder(in).setDecodingArea(decodingArea).decode();
                    util.assertBitmapsEqual(expected, decoded);
                }

                //test decode from byte array
                byte[] data = util.loadAssetFile(jp2Files[i]);
                decoded = new JP2Decoder(data).setDecodingArea(decodingArea).decode();
                util.assertBitmapsEqual(expected, decoded);
            }
        }
    }

    /*
      Try decoding an image with decoding area specified badly.
     */
    @Test
    public void testDecodingAreaError() throws Exception {
        String jp2File = "lena.jp2"; //dimensions 512x512

        for (Rect decodingArea : new Rect[]{
                //too big width
                new Rect(0,0,513,512),
                //smaller width but right border outside the picture
                new Rect(500,0,550,512),
                //too big height
                new Rect(0,0,512,513),
                //smaller height but bottom border outside the picture
                new Rect(0,500,512,550),
        }) {
            //test decode from file
            File outFile = util.createFile(util.loadAssetFile(jp2File));
            Bitmap decoded = new JP2Decoder(outFile.getPath()).setDecodingArea(decodingArea).decode();
            assertNull("bitmap shouldn't have been decoded:", decoded);
            outFile.delete();

            //test decode from stream
            try (InputStream in = util.openAssetStream(jp2File)) {
                decoded = new JP2Decoder(in).setDecodingArea(decodingArea).decode();
                assertNull("bitmap shouldn't have been decoded", decoded);
            }

            //test decode from byte array
            byte[] data = util.loadAssetFile(jp2File);
            decoded = new JP2Decoder(data).setDecodingArea(decodingArea).decode();
            assertNull("bitmap shouldn't have been decoded", decoded);
        }
    }

    /*
      Decode a HTJ2K image, compare it with the expected results.
     */
    @Test
    public void testDecodeHTJ2K() throws Exception {
        String[] jp2Files = new String[] {"butterfly.jhc", "grok-ht.jhc", "oj-ht-byte.jph", "oj-ht-byte_causal.jhc", "tileoffset-ojph.jhc"};
        String[] expectedFiles = new String[] {"butterfly.png", "grok-ht.png", "oj-ht-byte.png", "oj-ht-byte_causal.png", "tileoffset-ojph.png"};


        for (int i = 0; i < jp2Files.length; i++) {
            Bitmap expected = util.loadAssetBitmap(expectedFiles[i]);

            //test decode from file
            File outFile = util.createFile(util.loadAssetFile(jp2Files[i]));

            Bitmap decoded = new JP2Decoder(outFile.getPath()).decode();
            assertFalse(decoded.hasAlpha());

            util.assertBitmapsEqual(expected, decoded);
            outFile.delete();

            //test decode from stream
            try (InputStream in = util.openAssetStream(jp2Files[i])) {
                decoded = new JP2Decoder(in).decode();
                util.assertBitmapsEqual(expected, decoded);
            }

            //test decode from byte array
            byte[] data = util.loadAssetFile(jp2Files[i]);
            decoded = new JP2Decoder(data).decode();
            util.assertBitmapsEqual(expected, decoded);
        }
    }

    /*
      Decode a transparent RGB image and a transparent greyscale image, compare them with the expected results.
      (Note: we don't store the expected results as PNG in this test, because Android stores transparent bitmaps
       with color components pre-multiplied with alpha. Due to different rounding mechanisms in OpenJPEG and Android,
       and possibly different Bitmap configs being used, this can lead to minor differences in pixel values when util.loading
       the same transparent image from PNG and JP2. So we store the expected results as RAW and util.load them using
       the same bitmap config as our library uses; this way we ensure that the rounding errors in both decoded and
       expected bitmap will be the same.)
     */
    @Test
    public void testDecodeTransparent() throws Exception {
        //test RGBA image
        Bitmap decoded = new JP2Decoder(util.loadAssetFile("transparent.jp2")).decode();
        assertTrue(decoded.hasAlpha());
        Bitmap expected = util.loadAssetRawBitmap("transparent.raw", decoded.getWidth(), decoded.getHeight());
        util.assertBitmapsEqual(expected, decoded);

        //test greyscale image with alpha
        decoded = new JP2Decoder(util.loadAssetFile("transparent-grey.jp2")).decode();
        assertTrue(decoded.hasAlpha());
        expected = util.loadAssetRawBitmap("transparent-grey.raw", decoded.getWidth(), decoded.getHeight());
        util.assertBitmapsEqual(expected, decoded);
    }

    /*
      Test decoder wrong input.
     */
    @Test
    public void testDecodeError() throws Exception {
        assertNull(new JP2Decoder(util.loadAssetFile("lena.png")).decode());
        assertNull(new JP2Decoder((byte[])null).decode());
        assertNull(new JP2Decoder(new byte[0]).decode());
        assertNull(new JP2Decoder(new byte[1]).decode());
        assertNull(new JP2Decoder(new byte[2]).decode());
        assertNull(new JP2Decoder(new byte[16000000]).decode());
        assertNull(new JP2Decoder((InputStream)null).decode());
        assertNull(new JP2Decoder((String)null).decode());

        //decode from wrong file
        File outFile = util.createFile(util.loadAssetFile("lena.png"));
        assertNull(new JP2Decoder(outFile.getPath()).decode());
        outFile.delete();
        //decode from non-existant file
        assertNull(new JP2Decoder(outFile.getPath()).decode());

        //incomplete JPEG 2000 file
        byte[] data = util.loadAssetFile("lena.jp2");
        assertNull(new JP2Decoder(Arrays.copyOf(data, data.length / 2)).decode());
    }

    @Test
    public void testSubsampling() throws Throwable {
        List<ExpectedHeader> dataList = new ArrayList<>();
        dataList.add(new ExpectedHeader("subsampling_1.jp2", 1280, 1024, false, 6, 6));
        dataList.add(new ExpectedHeader("subsampling_2.jp2", 640, 512, false, 6, 5));
        for (ExpectedHeader expected : dataList) {
            byte[] data = util.loadAssetFile(expected.file);
            JP2Decoder dec = new JP2Decoder(data);
            JP2Decoder.Header header = dec.readHeader();
            assertNotNull(expected.file + " header is null", header);
            assertEquals(expected.file + " header, Wrong width", expected.width, header.width);
            assertEquals(expected.file + " header, Wrong height", expected.height, header.height);
            assertEquals(expected.file + " header, Wrong alpha", expected.hasAlpha, header.hasAlpha);
            assertEquals(expected.file + " header, Wrong number of resolutions", expected.numResolutions, header.numResolutions);
            assertEquals(expected.file + " header, Wrong number of quality layers", expected.numQualityLayers, header.numQualityLayers);

            Bitmap jp2Bitmap = dec.decode();
            Bitmap pngBitmap = util.loadAssetBitmap(expected.file.replace(".jp2", ".png"));
            util.assertBitmapsEqual(expected.file, pngBitmap, jp2Bitmap);
        }
    }

    /*
     * Test reading header information.
     */
    @Test
    public void testReadHeader() throws Throwable {
        List<ExpectedHeader> dataList = new ArrayList<>();
        dataList.add(new ExpectedHeader("headerTest-r1-l1.jp2", 335, 151, false, 1, 1));
        dataList.add(new ExpectedHeader("headerTest-r2-l3.j2k", 335, 151, false, 2, 3));
        dataList.add(new ExpectedHeader("headerTest-r5-l1.j2k", 335, 151, false, 5, 1));
        dataList.add(new ExpectedHeader("headerTest-r7-l5.jp2", 335, 151, false, 7, 5));
        dataList.add(new ExpectedHeader("tiled-r6-l6.jp2", 2717, 3701, false, 6, 6));
        dataList.add(new ExpectedHeader("tiled-r6-l1.j2k", 2717, 3701, false, 6, 1));
        dataList.add(new ExpectedHeader("transparent.jp2", 175, 65, true, 6, 1));

        JP2Decoder.Header header;
        for (ExpectedHeader expected : dataList) {
            byte[] data = util.loadAssetFile(expected.file);
            for (int i = 0; i < 3; i++) {
                if (i == 0) {
                    //read header from byte array
                    header = new JP2Decoder(data).readHeader();
                }
                else if (i == 1) {
                    //read header from file
                    File f = util.createFile(data);
                    header = new JP2Decoder(f.getPath()).readHeader();
                    f.delete();
                } else {
                    //read header from input stream
                    header = new JP2Decoder(util.openAssetStream(expected.file)).readHeader();
                }

                assertNotNull(expected.file + ", Header is null", header);
                assertEquals(expected.file + ", Wrong width", expected.width, header.width);
                assertEquals(expected.file + ", Wrong height", expected.height, header.height);
                assertEquals(expected.file + ", Wrong alpha", expected.hasAlpha, header.hasAlpha);
                assertEquals(expected.file + ", Wrong number of resolutions", expected.numResolutions, header.numResolutions);
                assertEquals(expected.file + ", Wrong number of quality layers", expected.numQualityLayers, header.numQualityLayers);
            }
        }

        //test wrong data
        byte[] data = util.loadAssetFile("lena.png");
        //byte array
        header = new JP2Decoder(data).readHeader();
        assertNull(header);
        //file
        File f = util.createFile(data);
        header = new JP2Decoder(data).readHeader();
        assertNull(header);
        f.delete();
        //non-existant file
        header = new JP2Decoder(data).readHeader();
        assertNull(header);
        //input stream
        header = new JP2Decoder(util.openAssetStream("lena.png")).readHeader();
        assertNull(header);
        //null input
        assertNull(new JP2Decoder((byte[])null).readHeader());
        assertNull(new JP2Decoder((InputStream)null).readHeader());
        assertNull(new JP2Decoder((String)null).readHeader());

    }

    @Test
    public void testDisablePremultiplication() throws Exception {
        int[] decodedClean, decodedPremultiplied; //decoded jp2 data, pre-multiplication off and on
        int[] expectedClean, expectedPremultiplied; //loaded raw data, once clean, once run through pre-multiplication

        //premultiplication can't be disabled in pre-KitKat SDKs; in case of testing on such old SDKs
        //we test that we get premultiplied data even if un-premultiplied data was requested
        boolean isPremultiplicationOffSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // test with RGBA file, greyscale file with alpha, and opaque file
        for (String file : new String[]{"transparent", "transparent-grey", "lena"}) {
            try {
                boolean transparent = file.startsWith("transparent");
                //decode transparent bitmap, pre-multiplication off
                Bitmap bmpClean = new JP2Decoder(util.loadAssetFile(file + ".jp2"))
                        .disableBitmapPremultiplication()
                        .decode();

                if (!isPremultiplicationOffSupported) {
                    fail("Disabling pre-multiplication is not supported on API " + Build.VERSION.SDK_INT + ", yet exception was not thrown");
                }

                assertEquals("error in " + file + ".jp2 alpha", transparent, bmpClean.hasAlpha());
                //isPremultiplied() should return false for both transparent and opaque bitmap
                assertFalse("error in " + file + ".jp2 premultiplication", bmpClean.isPremultiplied());
                decodedClean = new int[bmpClean.getWidth() * bmpClean.getHeight()];
                bmpClean.getPixels(decodedClean, 0, bmpClean.getWidth(), 0, 0, bmpClean.getWidth(), bmpClean.getHeight());

                //decode transparent bitmap, pre-multiplication on
                Bitmap bmpPremultiplied = new JP2Decoder(util.loadAssetFile(file + ".jp2")).decode();
                assertEquals("error in " + file + ".jp2 alpha", transparent, bmpPremultiplied.hasAlpha());
                //isPremultiplied() should return true for transparent bitmap; false for opaque one
                assertEquals("error in " + file + ".jp2 premultiplication", transparent, bmpPremultiplied.isPremultiplied());
                decodedPremultiplied = new int[bmpPremultiplied.getWidth() * bmpPremultiplied.getHeight()];
                bmpPremultiplied.getPixels(decodedPremultiplied, 0, bmpPremultiplied.getWidth(), 0, 0, bmpPremultiplied.getWidth(), bmpPremultiplied.getHeight());

                //load expected bitmap, pre-multiplication off
                expectedClean = util.loadAssetRawPixels(file + ".raw");
                //load expected bitmap, pre-multiplication on
                expectedPremultiplied = new int[expectedClean.length];
                util.loadAssetRawBitmap(file + ".raw", bmpPremultiplied.getWidth(), bmpPremultiplied.getHeight())
                    .getPixels(expectedPremultiplied, 0, bmpPremultiplied.getWidth(), 0, 0, bmpPremultiplied.getWidth(), bmpPremultiplied.getHeight());

                //compare the data
                if (transparent) {
                    //make sure clean and pre-multiplied data is loaded as expected
                    util.assertBitmapsEqual("error in " + file + ".jp2", expectedClean, decodedClean);
                    util.assertBitmapsEqual("error in " + file + ".jp2", expectedPremultiplied, decodedPremultiplied);
                    //make sure clean and pre-multiplied data is different from each other
                    assertFalse("error in " + file + ".jp2 - premultiplied and non-premultiplied data should not be the same",
                            Arrays.equals(expectedClean, decodedPremultiplied));
                    assertFalse("error in " + file + ".jp2 - premultiplied and non-premultiplied data should not be the same",
                            Arrays.equals(expectedPremultiplied, decodedClean));
                } else {
                    //make sure all the data is the same
                    util.assertBitmapsEqual("error in " + file + ".jp2", expectedClean, decodedClean);
                    util.assertBitmapsEqual("error in " + file + ".jp2", expectedPremultiplied, decodedPremultiplied);
                    util.assertBitmapsEqual("error in " + file + ".jp2", expectedPremultiplied, decodedClean);
                    util.assertBitmapsEqual("error in " + file + ".jp2", expectedClean, decodedPremultiplied);
                }
            } catch (UnsupportedOperationException e) {
                assertFalse("Disabling pre-multiplication should be supported on API " + Build.VERSION.SDK_INT + ", yet exception was thrown", isPremultiplicationOffSupported);
            }
        }
    }

    /*
        Check that an exception is thrown when bad parameters are used.
     */
    @Test
    public void testParamsErrors() throws Exception {
        JP2Decoder dec = new JP2Decoder((byte[])null);
        try {
            dec.setLayersToDecode(-1);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {
        }
        try {
            dec.setLayersToDecode(Integer.MIN_VALUE);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {
        }
        try {
            dec.setSkipResolutions(-1);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {
        }
        try {
            dec.setSkipResolutions(Integer.MIN_VALUE);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {
        }
        for (Rect decodingArea : new Rect[]{
                //negative coordinates
                new Rect(-1, 1, 10, 10),
                new Rect(1, -1, 10, 10),
                new Rect(1, 1, -10, 10),
                new Rect(1, 1, 10, -10),
                //zero-height
                new Rect(0, 100, 100, 100),
                //zero-width
                new Rect(100, 0, 100, 100),
                //negative width
                new Rect(100, 1, 50, 100),
                //negative height
                new Rect(100, 100, 150, 50),
        }) {
            try {
                dec.setDecodingArea(decodingArea);
                fail("Exception should have been thrown");
            } catch (IllegalArgumentException ignored) {
            }
        }

        //test too small decoding area for the existing skipResolutions parameter
        dec.setSkipResolutions(3);
        try {
            dec.setDecodingArea(new Rect(5, 5, 13, 12));
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {
        }
        try {
            dec.setDecodingArea(new Rect(17, 17, 24, 25));
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {
        }
        dec.setSkipResolutions(0);
        //test too small skipResolutions the existing decodingArea parameter
        dec.setDecodingArea(new Rect(0, 0, 31, 31));
        for (int i = 1; i < 5; i++) {
            dec.setSkipResolutions(i); //should work fine
        }
        try {
            dec.setSkipResolutions(5);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {
        }
        try {
            dec.setSkipResolutions(15);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static class LRTestParams {
        String file;
        Integer skipResolutions;
        Integer decodeLayers;

        public LRTestParams(final String file, final Integer skipResolutions, final Integer decodeLayers) {
            this.file = file;
            this.skipResolutions = skipResolutions;
            this.decodeLayers = decodeLayers;
        }
    }

    @Test
    public void testLayersAndResolutions() throws Exception {
        byte[] file = util.loadAssetFile("decodeTest.jp2"); //7 resolutions, 5 quality layers, 335x151
        List<LRTestParams> params = new ArrayList<>();
        params.add(new LRTestParams("decodeTest-r3.png", 3, null));
        params.add(new LRTestParams("decodeTest-r3.png", 3, 0));
        params.add(new LRTestParams("decodeTest-r6.png", 6, null));
        params.add(new LRTestParams("decodeTest-r6.png", 6, 0));
        params.add(new LRTestParams("decodeTest-l1.png", 0, 1));
        params.add(new LRTestParams("decodeTest-l1.png", null, 1));
        params.add(new LRTestParams("decodeTest-l4.png", 0, 4));
        params.add(new LRTestParams("decodeTest-l4.png", null, 4));
        params.add(new LRTestParams("decodeTest-r1l1.png", 1, 1));
        params.add(new LRTestParams("decodeTest-r1l4.png", 1, 4));
        params.add(new LRTestParams("decodeTest-r2l5.png", 2, 5));

        for (LRTestParams param : params) {
            Bitmap expected = util.loadAssetBitmap(param.file);
            JP2Decoder dec = new JP2Decoder(file);
            if (param.decodeLayers != null) dec.setLayersToDecode(param.decodeLayers);
            if (param.skipResolutions != null) dec.setSkipResolutions(param.skipResolutions);
            Bitmap decoded = dec.decode();
            util.assertBitmapsEqual("Error in " + param.file, expected, decoded);
        }
    }

    /*
      Here we test that the expected part of the image is returned when both decodingArea and
      skipResolutions is used together. The decoding area selects the part of the image to be
      decoded, and then skipResolutions reduces its size.

      (The decodeLayers and decodingArea parameters don't really interfere with one another;
      we're just testing both still work when used together.)
     */
    @Test
    public void testDecodingAreaWithLayersAndResolutions() throws Exception {
        byte[] file = util.loadAssetFile("decodeTest.jp2"); //7 resolutions, 5 quality layers, 335x151
        List<LRTestParams> params = new ArrayList<>();
        params.add(new LRTestParams("decodeTest-r3.png", 3, null));
        params.add(new LRTestParams("decodeTest-l1.png", null, 1));
        params.add(new LRTestParams("decodeTest-r1l4.png", 1, 4));

        Rect[] decodingAreas = new Rect[]{
                new Rect(0, 0, 335, 151), //full width and height
                new Rect(10, 10, 325, 141), //10 px taken from every side
                new Rect(7, 17, 256, 25), //height 8 px; minimum for skipResolutions 3
                new Rect(7, 17, 15, 151), //height 8 px; minimum for skipResolutions 3
        };

        for (LRTestParams param : params) {
            for (Rect decodingArea : decodingAreas) {
                Bitmap expected = util.loadAssetBitmap(param.file, util.scaleDownDecodingArea(decodingArea, param.skipResolutions));
                JP2Decoder dec = new JP2Decoder(file);
                if (param.decodeLayers != null) dec.setLayersToDecode(param.decodeLayers);
                if (param.skipResolutions != null) dec.setSkipResolutions(param.skipResolutions);
                dec.setDecodingArea(decodingArea);
                Bitmap decoded = dec.decode();
                util.assertBitmapsEqual("Error in " + param.file, expected, decoded);
            }
        }
    }

    @Test
    public void testDecodeMultithreaded() throws Throwable {
        //test decoding in multiple (4) threads.
        //We load 4 different images, decode them repeatedly in 4 threads and check that we
        //always get the expected result.
        DecoderThread t1 = new DecoderThread("lena.jp2", "lena.png");
        DecoderThread t2 = new DecoderThread("lena-rotated90.jp2", "lena-rotated90.png");
        DecoderThread t3 = new DecoderThread("lena-rotated180.jp2", "lena-rotated180.png");
        DecoderThread t4 = new DecoderThread("lena-rotated270.jp2", "lena-rotated270.png");

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        while (!t1.finished || !t2.finished || !t3.finished || !t4.finished) {
            t1.checkError();
            t2.checkError();
            t3.checkError();
            t4.checkError();
            try {Thread.sleep(500);}catch (InterruptedException ignored){}
        }
        t1.checkError();
        t2.checkError();
        t3.checkError();
        t4.checkError();
    }

    class DecoderThread extends Thread {
        private static final int REPEATS = 5;
        String pngFile;
        String jp2File;
        boolean finished = false;
        Throwable error = null;
        Bitmap expected;
        byte[] encoded;

        DecoderThread(final String jp2File, final String pngFile) throws Exception {
            this.pngFile = pngFile;
            this.jp2File = jp2File;
            expected = util.loadAssetBitmap(pngFile);
            encoded = util.loadAssetFile(jp2File);
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < REPEATS; i++) {
                    //test byte array
                    Bitmap decoded = new JP2Decoder(encoded).decode();
                    util.assertBitmapsEqual("decoded " + jp2File + " is different from " + pngFile, expected, decoded);

                    //test decode from file
                    File outFile = util.createFile(encoded);
                    decoded = new JP2Decoder(outFile.getPath()).decode();
                    util.assertBitmapsEqual("decoded " + jp2File + " is different from " + pngFile, expected, decoded);
                    outFile.delete();

                    //test decode from stream
                    try (InputStream in = util.openAssetStream(jp2File)) {
                        decoded = new JP2Decoder(in).decode();
                        util.assertBitmapsEqual("decoded " + jp2File + " is different from " + pngFile, expected, decoded);
                    }
                }
            } catch (Throwable e) {
                error = e;
            }
            finished = true;
        }

        void checkError() throws Throwable {
            if (error != null) throw error;
        }
    }

    private static class ExpectedHeader extends JP2Decoder.Header {
        public String file;

        public ExpectedHeader(final String file, final int width, final int height, final boolean hasAlpha, final int numResolutions,
                              final int numQualityLayers) {
            this.file = file;
            this.width = width;
            this.height = height;
            this.hasAlpha = hasAlpha;
            this.numResolutions = numResolutions;
            this.numQualityLayers = numQualityLayers;
        }
    }
}
