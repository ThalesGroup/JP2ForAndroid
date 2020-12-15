package com.gemalto.jp2.test;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.gemalto.jp2.JP2Decoder;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ImageView imgView = findViewById(R.id.image);
        imgView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //we want to decode the JP2 only when the layout is created and we know the ImageView size
                imgView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                new DecodeJp2AsyncTask(imgView).execute();
            }
        });
    }

    private void close(Closeable obj) {
        try {
            if (obj != null) obj.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * This is an example of how to take advantage of the {@link JP2Decoder#setSkipResolutions(int)} method in a multi-resolution JP2 image.
     * We take the size of the ImageView component, the resolution of the image, the number of available resolutions, and we determine how many
     * of them we can skip without losing any detail.
     *
     * We know that each successive JP2 resolution is half of the previous resolution. So if the first (highest) resolution is 4000x3000, then
     * the next resolution (if present) will be 2000x1500, the next one 1000x750, and so on.
     *
     * Therefore if the ImageView size is 1800x1800 for example, we know that we can skip one resolution. (The 2000x1500 version is bigger - at least
     * in one dimension - than 1800x1800. The 1000x750 is smaller and we would lose image details.)
     */
    private class DecodeJp2AsyncTask extends AsyncTask<Void, Void, Bitmap> {
        private ImageView view;
        private int width, height;

        public DecodeJp2AsyncTask(final ImageView view) {
            this.view = view;
            //get the size of the ImageView
            width = view.getWidth();
            height = view.getHeight();
        }

        @Override
        protected Bitmap doInBackground(final Void... voids) {
            Log.d(TAG, String.format("View resolution: %d x %d", width, height));
            Bitmap ret = null;
            InputStream in = null;
            try {
                in = getAssets().open("balloon.jp2");

                //create a new JP2 decoder object
                JP2Decoder decoder = new JP2Decoder(in);

                //read image information only, but don't decode the actual image
                JP2Decoder.Header header = decoder.readHeader();

                //get the size of the image
                int imgWidth = header.width;
                int imgHeight = header.height;
                Log.d(TAG, String.format("JP2 resolution: %d x %d", imgWidth, imgHeight));

                //we halve the resolution until we go under the ImageView size or until we run out of the available JP2 image resolutions
                int skipResolutions = 1;
                while (skipResolutions < header.numResolutions) {
                    imgWidth >>= 1;
                    imgHeight >>= 1;
                    if (imgWidth < width && imgHeight < height) break;
                    else skipResolutions++;
                }

                //we break the loop when skipResolutions goes over the correct value
                skipResolutions--;
                Log.d(TAG, String.format("Skipping %d resolutions", skipResolutions));

                //set the number of resolutions to skip
                if (skipResolutions > 0) decoder.setSkipResolutions(skipResolutions);

                //decode the image
                ret = decoder.decode();
                Log.d(TAG, String.format("Decoded at resolution: %d x %d", ret.getWidth(), ret.getHeight()));
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                close(in);
            }
            return ret;
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            if (bitmap != null) {
                view.setImageBitmap(bitmap);
            }
        }
    }
}
