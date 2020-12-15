package com.gemalto.jp2;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TestJp2Encoder {
    // Context of the app under test.
    private Context ctx;
    private Util util;

    @Before
    public void init() {
        ctx = ApplicationProvider.getApplicationContext();
        util = new Util(ctx);
    }

    /*
      Encode an image with all RGB colors into JP2, decode it and compare with the original.
     */
    @Test
    public void testEncodeAllColorsLossless() throws Exception {
        Bitmap bmp = util.loadAssetBitmap("fullrgb.png");

        JP2Encoder enc = new JP2Encoder(bmp);
        byte[] encoded = enc.encode();
        assertNotNull(encoded);
        Bitmap bmpDecoded = new JP2Decoder(encoded).decode();
        assertNotNull(bmpDecoded);

        util.assertBitmapsEqual(bmp, bmpDecoded);
    }

    /*
      Encode several normal images, decode them, compare them to the originals.
      Several small sizes are tested to make sure the library correctly sets the Number of Resolutions parameter.
      (OpenJPEG uses 6 resolutions by default, but it throws an error if the image is smaller than 32x32. For
      such small images the number of resolutions must be smaller.)
     */
    @Test
    public void testEncodeSimple() throws Exception {
        String[] images = new String[] {"lena.png", "lena-grey.png", "1x1.png", "2x2.png", "3x3.png", "32x15.png", "32x16.png"};

        for (int i = 0; i < images.length; i++) {
            Bitmap expected = util.loadAssetBitmap(images[i]);

            //test encode to file
            File outFile = new File(ctx.getFilesDir(), "tmp.tmp");
            assertTrue(new JP2Encoder(expected).encode(outFile.getPath()));
            byte[] encoded = util.loadFile(outFile.getPath());
            assertNotNull(encoded);
            assertTrue(JP2Decoder.isJPEG2000(encoded));
            Bitmap decoded = new JP2Decoder(encoded).decode();
            assertFalse(decoded.hasAlpha());
            util.assertBitmapsEqual(expected, decoded);
            outFile.delete();

            //test encode to stream
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            assertTrue(new JP2Encoder(expected).encode(out) > 0);
            encoded = out.toByteArray();
            assertNotNull(encoded);
            assertTrue(JP2Decoder.isJPEG2000(encoded));
            decoded = new JP2Decoder(encoded).decode();
            assertFalse(decoded.hasAlpha());
            util.assertBitmapsEqual(expected, decoded);

            //test encode to byte array
            encoded = new JP2Encoder(expected).encode();
            assertNotNull(encoded);
            assertTrue(JP2Decoder.isJPEG2000(encoded));
            decoded = new JP2Decoder(encoded).decode();
            assertFalse(decoded.hasAlpha());
            util.assertBitmapsEqual(expected, decoded);
        }
    }

    /*
      Encode transparent image (full-RGB and greyscale), decode it compare to the original.
      (The original is loaded from raw bytes to avoid rounding errors introduced by Android
      when decoding a transparent PNG due to pre-multiplied color values and other such nonsense.)
     */
    @Test
    public void testEncodeTransparent() throws Exception {
        String[] images = new String[] {"transparent.raw", "transparent-grey.raw"};
        int width = 175;
        int height = 65;

        for (int i = 0; i < images.length; i++) {
            Bitmap expected = util.loadAssetRawBitmap(images[i], width, height);

            //test encode to file
            File outFile = new File(ctx.getFilesDir(), "tmp.tmp");
            assertTrue(new JP2Encoder(expected).encode(outFile.getPath()));
            byte[] encoded = util.loadFile(outFile.getPath());
            assertNotNull(encoded);
            assertTrue(JP2Decoder.isJPEG2000(encoded));
            Bitmap decoded = new JP2Decoder(encoded).decode();
            assertTrue(decoded.hasAlpha());
            util.assertBitmapsEqual(expected, decoded);
            outFile.delete();

            //test encode to stream
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            assertTrue(new JP2Encoder(expected).encode(out) > 0);
            encoded = out.toByteArray();
            assertNotNull(encoded);
            assertTrue(JP2Decoder.isJPEG2000(encoded));
            decoded = new JP2Decoder(encoded).decode();
            assertTrue(decoded.hasAlpha());
            util.assertBitmapsEqual(expected, decoded);

            //test encode to byte array
            encoded = new JP2Encoder(expected).encode();
            assertNotNull(encoded);
            assertTrue(JP2Decoder.isJPEG2000(encoded));
            decoded = new JP2Decoder(encoded).decode();
            assertTrue(decoded.hasAlpha());
            util.assertBitmapsEqual(expected, decoded);
        }
    }

    private static final byte[] JP2_RFC3745_MAGIC = new byte[]{(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0c, (byte)0x6a, (byte)0x50, (byte)0x20, (byte)0x20, (byte)0x0d, (byte)0x0a, (byte)0x87, (byte)0x0a};
    private static final byte[] JP2_MAGIC = new byte[]{(byte)0x0d, (byte)0x0a, (byte)0x87, (byte)0x0a};
    private static final byte[] J2K_CODESTREAM_MAGIC = new byte[]{(byte)0xff, (byte)0x4f, (byte)0xff, (byte)0x51};

    //does array1 start with contents of array2?
    private static boolean startsWith(byte[] array1, byte[] array2) {
        for (int i = 0; i < array2.length; i++) {
            if (array1[i] != array2[i]) return false;
        }
        return true;
    }

    /*
        Check that the requested output format is produced.
     */
    @Test
    public void testEncodeFormat() throws Exception {
        Bitmap expected = util.loadAssetBitmap("lena.png");
        //test jp2 format
        byte[] data = new JP2Encoder(expected).setOutputFormat(JP2Encoder.FORMAT_JP2).encode();
        assertTrue("JP2 header not found", startsWith(data, JP2_MAGIC) || startsWith(data, JP2_RFC3745_MAGIC));
        Bitmap decoded = new JP2Decoder(data).decode();
        util.assertBitmapsEqual(expected, decoded);

        //test j2k format
        data = new JP2Encoder(expected).setOutputFormat(JP2Encoder.FORMAT_J2K).encode();
        assertTrue("JP2 header not found", startsWith(data, J2K_CODESTREAM_MAGIC));
        decoded = new JP2Decoder(data).decode();
        util.assertBitmapsEqual(expected, decoded);
    }

    /*
        Check that an exception is thrown when bad parameters are used.
     */
    @Test
    public void testEncodeParamsErrors() throws Exception {
        try {
            //null bitmap
            new JP2Encoder(null);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {}

        Bitmap expected = util.loadAssetBitmap("lena.png");

        try {
            //undefined output format
            new JP2Encoder(expected).setOutputFormat(3);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {}

        try {
            //zero is not allowed
            new JP2Encoder(expected).setCompressionRatio(1, 0, 3);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {}

        try {
            //negative numbers not allowed is not allowed
            new JP2Encoder(expected).setCompressionRatio(-1, 2, 3);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {}

        try {
            //negative numbers not allowed
            new JP2Encoder(expected).setVisualQuality(30, 20, -10);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {}

        JP2Encoder enc = new JP2Encoder(expected).setCompressionRatio(20, 10, 1);
        try {
            enc.setVisualQuality(10, 20, 30);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {}

        enc = new JP2Encoder(expected).setVisualQuality(10, 20, 30);
        try {
            enc.setCompressionRatio(20, 10, 1);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {}

        for (int resolutions : new int[]{0, 32}) {
            try {
                new JP2Encoder(expected).setNumResolutions(resolutions);
                fail("Exception should have been thrown");
            } catch (IllegalArgumentException ignored) {}
        }
    }

    /*
       Check that correct number of resolutions is accepted and too high number of resolutions is rejected
       for multiple image sizes.
     */
    @Test
    public void testNumResolutions() {
        //triplets - (width, height, maxResolutions)
        int[][] testData = new int[][]{
                new int[]{1, 1, 1},
                new int[]{2, 1, 1},
                new int[]{2, 2, 2},
                new int[]{5, 1, 1},
                new int[]{5, 3, 2},
                new int[]{5, 4, 3},
                new int[]{63, 63, 6},
                new int[]{64, 63, 6},
                new int[]{64, 64, 7},
                new int[]{1023, 1024, 10},
                new int[]{1024, 1024, 11},
        };

        for (int[] data : testData) {
            int width = data[0];
            int height = data[1];
            int maxResolutions = data[2];
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
            JP2Encoder enc = new JP2Encoder(bmp);
            for (int res = 1; res <= maxResolutions; res++) {
                try {
                    enc.setNumResolutions(res);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    fail(String.format("%d should be an acceptable number of resolutions for image size %d x %d, but an exception was thrown", res, width, height));
                }
            }
            for (int res = maxResolutions + 1; res <= 31; res++) {
                try {
                    enc.setNumResolutions(res);
                    fail(String.format("%d is not an acceptable number of resolutions for image size %d x %d. An exception should have been thrown", res, width, height));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    /*
      Test that the visual quality setting is working as it should.
     */
    @Test
    public void testQuality() throws Exception {
        Bitmap orig = util.loadAssetBitmap("lena.png");

        //start with lossless image
        byte[] encoded = new JP2Encoder(orig).setVisualQuality(0).encode();
        assertNotNull(encoded);
        int lastSize = encoded.length;
        Bitmap decoded = new JP2Decoder(encoded).decode();
        assertNotNull(decoded);
        util.assertBitmapsEqual(orig, decoded);

        //continue with lossy compression - decrease quality and check that PSNR is about right and file size decrease as well.
        //we only use "reasonable" PSNR values here; PSNR > 70 is pretty much unattainable and PSNR < 20 is mostly just garbage
        for (float quality : new float[]{70, 60, 50, 40, 30, 20}) {
            encoded = new JP2Encoder(orig).setVisualQuality(quality).encode();
            assertNotNull(encoded);
            assertTrue("Lower quality (" + quality + ") should lead to smaller file", encoded.length < lastSize);
            lastSize = encoded.length;

            decoded = new JP2Decoder(encoded).decode();
            assertNotNull(decoded);
            assertPsnr(quality, orig, decoded);
        }

        //test multiple qualities

        //use qualities in an unsorted order (the library should sort it)
        float[] qualities = new float[]{30, 50, 20};
        encoded = new JP2Encoder(orig).setVisualQuality(qualities).encode();
        JP2Decoder.Header header = new JP2Decoder(encoded).readHeader();
        assertEquals("wrong number of quality layers", header.numQualityLayers, qualities.length);

        float[] sortedQualities = Arrays.copyOf(qualities, qualities.length);
        Arrays.sort(sortedQualities);
        //test partial decoding
        for (int i = 0; i < qualities.length; i++) {
            decoded = new JP2Decoder(encoded).setLayersToDecode(i + 1).decode();
            assertPsnr(sortedQualities[i], orig, decoded);
        }
        //test full decoding
        //all layers (implicit)
        decoded = new JP2Decoder(encoded).decode();
        assertPsnr(sortedQualities[sortedQualities.length - 1], orig, decoded);
        //all layers (explicit)
        decoded = new JP2Decoder(encoded).setLayersToDecode(0).decode();
        assertPsnr(sortedQualities[sortedQualities.length - 1], orig, decoded);
        //too high number of layers
        decoded = new JP2Decoder(encoded).setLayersToDecode(qualities.length + 1).decode();
        assertPsnr(sortedQualities[sortedQualities.length - 1], orig, decoded);
    }

    private void assertPsnr(double expectedPsnr, Bitmap orig, Bitmap decoded) {
        assertPsnr(null, expectedPsnr, orig, decoded);
    }

    private void assertPsnr(String message, double expectedPsnr, Bitmap orig, Bitmap decoded) {
        assertPsnr(message, expectedPsnr, 0.1, orig, decoded);
    }

    private void assertPsnr(String message, double expectedPsnr, double maxDiff, Bitmap orig, Bitmap decoded) {
        double psnr = psnr(orig, decoded);
        double psnrDiff = Math.abs(1 - psnr / expectedPsnr);
        assertTrue((message == null ? "" : message + "; ") + String.format("expected PSNR %f, actual PSNR %f (difference %d %%; max allowed difference is %d %%)", expectedPsnr, psnr, (int)(psnrDiff * 100), (int)(maxDiff * 100)), psnrDiff <= maxDiff);
    }

    /*
      Test that the compression ratio setting is working as it should. First check, that the lossless setting produces
      identical image, then go to lossy compression, increase the ratio and check that the file size is close to the
      expected value.
     */
    @Test
    public void testRatio() throws Exception {
        Bitmap orig = util.loadAssetBitmap("lena.png");

        //start with lossless image
        byte[] encoded = new JP2Encoder(orig).setCompressionRatio(1).encode();
        assertNotNull(encoded);
        Bitmap decoded = new JP2Decoder(encoded).decode();
        assertNotNull(decoded);
        util.assertBitmapsEqual(orig, decoded);

        //continue with lossy compression - increase compression ratio and check the file size is as expected
        int allowedSizeDifference = 5; //we allow maximum 5 % difference between expected and actual size
        for (int ratio : new int[]{2, 10, 20, 50, 100, 1000, 2000}) {
            //use J2K file format as it has less overhead to throw off our computation
            encoded = new JP2Encoder(orig).setOutputFormat(JP2Encoder.FORMAT_J2K).setCompressionRatio(ratio).encode();
            assertNotNull(encoded);
            //original image data size is width x height x number_of_bytes_per_pixel (3)
            int expectedSize = (orig.getWidth() * orig.getHeight() * 3) / ratio;
            double sizeDiff = Math.abs(1 - (encoded.length * 1.0 / expectedSize)) * 100; //size difference in percents
            assertTrue(String.format("Expected approximate size %d bytes for ratio %d, but got %d bytes, which is %.2f %% off. "
                                     + "Maximum allowed difference is %d %%.", expectedSize, ratio, encoded.length, sizeDiff, allowedSizeDifference),
                        sizeDiff <= allowedSizeDifference);

            //test that the result can actually be decoded
            decoded = new JP2Decoder(encoded).decode();
            assertNotNull(decoded);
        }

        //test multiple ratios (check that partial decoding of multiple quality layers yields the same psnr as a single-layer image)
        float[] ratios = new float[]{30,20,40};
        encoded = new JP2Encoder(orig).setCompressionRatio(ratios).encode();

        //sort the ratios
        float[] sortedRatios = Arrays.copyOf(ratios, ratios.length);
        //reverse the sorted array (ratios must be sorted in descending order)
        for (int i = 0; i < sortedRatios.length / 2; i++) {
            float tmp = sortedRatios[i];
            sortedRatios[i] = sortedRatios[sortedRatios.length - i - 1];
            sortedRatios[sortedRatios.length - i - 1] = tmp;
        }

        //we get the psnr values for each ratio - we encode a single quality layer image and get its PSNR
        double[] expectedPsnr = new double[ratios.length];
        for (int i = 0; i < ratios.length; i++) {
            byte[] tmp = new JP2Encoder(orig).setCompressionRatio(sortedRatios[i]).encode();
            expectedPsnr[i] = psnr(new JP2Decoder(tmp).decode(), orig);
        }

        //now check that we get the correct psnr in partial decode
        JP2Decoder dec = new JP2Decoder(encoded);
        for (int i = 0; i < sortedRatios.length; i++) {
            decoded = dec.setLayersToDecode(i + 1).decode();
            assertPsnr(expectedPsnr[i], orig, decoded);
        }
        //test full decoding
        //all layers (implicit)
        decoded = new JP2Decoder(encoded).decode();
        assertPsnr(expectedPsnr[sortedRatios.length - 1], orig, decoded);
        //all layers (explicit)
        decoded = new JP2Decoder(encoded).setLayersToDecode(0).decode();
        assertPsnr(expectedPsnr[sortedRatios.length - 1], orig, decoded);
        //too high number of layers
        decoded = new JP2Decoder(encoded).setLayersToDecode(sortedRatios.length + 1).decode();
        assertPsnr(expectedPsnr[sortedRatios.length - 1], orig, decoded);

    }

    /* test that multiple resolutions is correctly encoded */
    @Test
    public void testResolutions() throws Exception {
        Bitmap bmp = Bitmap.createBitmap(551, 645, Bitmap.Config.ARGB_8888);
        int[] resCount = new int[]{1, 3, 7, 9};

        for (int numResolutions : resCount) {
            byte[] encoded = new JP2Encoder(bmp).setNumResolutions(numResolutions).encode();
            int expectedWidth = bmp.getWidth();
            int expectedHeight = bmp.getHeight();
            for (int i = 0; i < numResolutions; i++) {
                if (i > 0) {
                    expectedWidth = (expectedWidth + 1) >> 1;
                    expectedHeight = (expectedHeight + 1) >> 1;
                }
                Bitmap decoded = new JP2Decoder(encoded).setSkipResolutions(i).decode();
                assertNotNull(decoded);
                assertEquals("Wrong width", expectedWidth, decoded.getWidth());
                assertEquals("Wrong height", expectedHeight, decoded.getHeight());
            }
            //test too high number of resolutions to skip - should decode the lowest available resolution
            Bitmap decoded = new JP2Decoder(encoded).setSkipResolutions(numResolutions).decode();
            assertNotNull(decoded);
            assertEquals("Wrong width", expectedWidth, decoded.getWidth());
            assertEquals("Wrong height", expectedHeight, decoded.getHeight());
        }
    }

    /* Test that multiple resolutions and quality layers combine well - decode all quality layers for each resolution,
       make sure that the quality is as expected (compute PSNR compared to a lossless compression at the same resolution).
     */
    @Test
    public void testQualityLayersAndResolutions() throws Exception {
        Bitmap orig = util.loadAssetBitmap("encodeTest.png");
        float[] qualities = new float[] {20, 30, 40};
        int resCount = 3;
        JP2Encoder enc = new JP2Encoder(orig).setVisualQuality(qualities).setNumResolutions(resCount);
        byte[] encoded = enc.encode();
        //test that encoding into a file produces the same result
        File encodedFile = new File(ctx.getFilesDir(), "test.jp2");
        assertTrue("encoding into file failed", enc.encode(encodedFile.getPath()));
        assertArrayEquals("encoding into file and byte array produced different data", encoded, util.loadFile(encodedFile.getPath()));
        encodedFile.delete();

        //encode lossless - as reference -  with the same number of resolutions
        byte[] encodedLossless = new JP2Encoder(orig).setNumResolutions(resCount).encode();

        for (int skipRes = 0; skipRes < resCount; skipRes++) {
            Bitmap origResized = new JP2Decoder(encodedLossless).setSkipResolutions(skipRes).decode();
            for (int numLayers = 0; numLayers < qualities.length; numLayers++) {
                Bitmap decoded = new JP2Decoder(encoded).setSkipResolutions(skipRes).setLayersToDecode(numLayers + 1).decode();
                //We allow bigger PSNR differences in lower resolutions because OpenJPEG doesn't hit the target quality so precisely there.
                //This value is good for the selected image and quality values - in case either is changed, this might have to be tweaked.
                double maxPsnrDiff = 0.1 * (skipRes + 1);
                assertPsnr(String.format("bad PSNR for skipRes = %d, numLayers = %d", skipRes, numLayers), qualities[numLayers], maxPsnrDiff, origResized, decoded);
            }
        }
    }

    private double psnr(Bitmap bmp1, Bitmap bmp2) {
        assertEquals("bitmaps have different width", bmp1.getWidth(), bmp2.getWidth());
        assertEquals("bitmaps have different height", bmp1.getHeight(), bmp2.getHeight());
        int[] pixels1 = new int[bmp1.getWidth() * bmp1.getHeight()];
        int[] pixels2 = new int[bmp2.getWidth() * bmp2.getHeight()];
        bmp1.getPixels(pixels1, 0, bmp1.getWidth(), 0, 0, bmp1.getWidth(), bmp1.getHeight());
        bmp2.getPixels(pixels2, 0, bmp2.getWidth(), 0, 0, bmp2.getWidth(), bmp2.getHeight());
        return psnr(pixels1, pixels2);
    }

    private double psnr(int[] pixels1, int[] pixels2) {
        double mse = meanSquareError(pixels1, pixels2);
        return 20 * Math.log10(255) - 10 * Math.log10(mse);
    }

    private double meanSquareError(int[] pixels1, int[] pixels2) {
        long acc = 0;
        for (int i = 0; i < pixels1.length; i++) {
            //we compare the differences only in the R,G,B channels, we ignore the alpha
            for (int offset = 8; offset < 32; offset += 8) {
                int diff = ((pixels1[i] >> offset) & 0xFF) - ((pixels2[i] >> offset) & 0xFF);
                acc += diff * diff;
            }
        }
        return acc * 1.0 / (pixels1.length * 3); //3 channels
    }


    @Test
    public void testEncodeMultithreaded() throws Throwable {
        //test encoding in multiple (4) threads.
        //We load 4 different images, encode them repeatedly in 4 threads and check that we
        //always get the expected result.
        EncoderThread t1 = new EncoderThread("lena.png");
        EncoderThread t2 = new EncoderThread("lena-rotated90.png");
        EncoderThread t3 = new EncoderThread("lena-rotated180.png");
        EncoderThread t4 = new EncoderThread("lena-rotated270.png");

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

    class EncoderThread extends Thread {
        private static final int REPEATS = 5;
        String pngFile;
        boolean finished = false;
        Throwable error = null;
        Bitmap expected;

        EncoderThread(final String pngFile) throws Exception {
            this.pngFile = pngFile;
            expected = util.loadAssetBitmap(pngFile);
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < REPEATS; i++) {
                    //test byte array
                    byte[] encoded = new JP2Encoder(expected).encode();
                    Bitmap decoded = new JP2Decoder(encoded).decode();
                    util.assertBitmapsEqual("encoded " + pngFile + " is different the original", expected, decoded);

                    //test encode to file
                    File outFile = File.createTempFile("testjp2", "tmp", ctx.getFilesDir());
                    assertTrue(new JP2Encoder(expected).encode(outFile.getPath()));
                    encoded = util.loadFile(outFile.getPath());
                    decoded = new JP2Decoder(encoded).decode();
                    util.assertBitmapsEqual("encoded " + pngFile + " is different the original", expected, decoded);
                    outFile.delete();

                    //test encode into stream
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    assertTrue(new JP2Encoder(expected).encode(out) > 0);
                    decoded = new JP2Decoder(out.toByteArray()).decode();
                    util.assertBitmapsEqual("encoded " + pngFile + " is different the original", expected, decoded);
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

}
