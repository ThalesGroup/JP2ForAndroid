package com.gemalto.jp2.test;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.gemalto.jp2.JP2Decoder;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ImageView imgView = findViewById(R.id.image);
        imgView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
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


    private class DecodeJp2AsyncTask extends AsyncTask<Void, Void, Bitmap> {
        private ImageView view;
        private int width, height;

        public DecodeJp2AsyncTask(final ImageView view) {
            this.view = view;
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

                JP2Decoder decoder = new JP2Decoder(in);
                JP2Decoder.Header header = decoder.readHeader();

                int skipResolutions = 1;
                int imgWidth = header.width;
                int imgHeight = header.height;

                Log.d(TAG, String.format("JP2 resolution: %d x %d", imgWidth, imgHeight));

                while (skipResolutions < header.numResolutions) {
                    imgWidth >>= 1;
                    imgHeight >>= 1;
                    if (imgWidth < width || imgHeight < height) break;
                    else skipResolutions++;
                }

                //we break the loop when skipResolutions goes over the correct value
                skipResolutions--;
                Log.d(TAG, String.format("Skipping %d resolutions", skipResolutions));

                if (skipResolutions > 0) decoder.setSkipResolutions(skipResolutions);

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
