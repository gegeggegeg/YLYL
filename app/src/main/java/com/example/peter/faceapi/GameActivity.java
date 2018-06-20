
package com.example.peter.faceapi;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;


import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;


import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.entity.FileEntity;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;


public class GameActivity extends YouTubeBaseActivity {

    private static final String TAG = "GameActivity";//log tage
    private String url = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0/detect";//Azure Face API url

    private Uri ImageUri; //image file saved uri
    private String YoutubeAPIKey = "AIzaSyDkPthW_w7svNz5hqzhFJmaChxojIFyV34"; //Youtube API key

    private YouTubePlayer.OnInitializedListener mInitializer;
    private YouTubePlayerView myoutubeplayerview;
    private ArrayList<YoutubevideoUnit> playlist;
    private int videoIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // enable File accessablity
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        //getting playlist from MainActivity;
        Intent getintent = getIntent();
        playlist = (ArrayList<YoutubevideoUnit>) getintent.getSerializableExtra("playlist");

        myoutubeplayerview = findViewById(R.id.playerview);
        mInitializer = new YouTubePlayer.OnInitializedListener() {
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
                Log.d(TAG, "onInitializationSuccess: Initializing success");
                videoIndex = 0;
                youTubePlayer.loadVideo(playlist.get(videoIndex).getVideoID(), playlist.get(videoIndex).getStartTime());
                youTubePlayer.play();
                playvideolist(youTubePlayer);
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {

            }
        };
        myoutubeplayerview.initialize(YoutubeAPIKey,mInitializer);
    }

    private void playvideolist(final YouTubePlayer youTubePlayer) {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                if(youTubePlayer.getCurrentTimeMillis() <= playlist.get(videoIndex).getEndTime()) {
                    handler.postDelayed(this, 1000);
                    Log.d(TAG, "run: current time: "+youTubePlayer.getCurrentTimeMillis());
                    Log.d(TAG, "run: endt time: " + playlist.get(videoIndex).getEndTime());
                } else {
                    videoIndex++;
                    handler.removeCallbacks(this);
                    if(videoIndex < playlist.size()) {
                        youTubePlayer.loadVideo(playlist.get(videoIndex).getVideoID(), playlist.get(videoIndex).getStartTime());
                        youTubePlayer.play();
                        playvideolist(youTubePlayer);
                    }else{
                        youTubePlayer.pause();
                    }
                }
            }
        }, 1000);
    }

    private Bitmap getBitmap(String path) {

        Uri uri = Uri.fromFile(new File(path));
        InputStream in = null;
        try {
            final int IMAGE_MAX_SIZE = 1200000; // 1.2MP
            in = getContentResolver().openInputStream(uri);

            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            in.close();


            int scale = 1;
            while ((o.outWidth * o.outHeight) * (1 / Math.pow(scale, 2)) >
                    IMAGE_MAX_SIZE) {
                scale++;
            }
            Log.d(TAG, "scale = " + scale + ", orig-width: " + o.outWidth + ", orig-height: " + o.outHeight);

            Bitmap b = null;
            in = getContentResolver().openInputStream(uri);
            if (scale > 1) {
                scale--;
                // scale to max possible inSampleSize that still yields an image
                // larger than target
                o = new BitmapFactory.Options();
                o.inSampleSize = scale;
                b = BitmapFactory.decodeStream(in, null, o);

                // resize to desired dimensions
                int height = b.getHeight();
                int width = b.getWidth();
                Log.d(TAG, "1th scale operation dimenions - width: " + width + ", height: " + height);

                double y = Math.sqrt(IMAGE_MAX_SIZE / (((double) width) / height));
                double x = (y / height) * width;

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, (int) x,
                        (int) y, true);
                b.recycle();
                b = scaledBitmap;

                System.gc();
            } else {
                b = BitmapFactory.decodeStream(in);
            }
            in.close();

            Log.d(TAG, "bitmap size - width: " + b.getWidth() + ", height: " +
                    b.getHeight());
            return b;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    class FaceAPItask extends AsyncTask<Void, Void, String> {

        private File file;

        public FaceAPItask(File file) {
            super();
            this.file = file;
        }

        @Override
        protected void onPostExecute(String result) {
            Double HappyScore = 0.0;
            JSONArray jsonArray = null;
            try{
                if (result != null)
                {
                    // Format and display the JSON response.
                    Log.d(TAG, "onPostExecute: REST Response:\n");

                    if (result.charAt(0) == '[') {
                        JSONArray jsonArray2 = new JSONArray(result);
                        Log.d(TAG, "onPostExecute: "+jsonArray2.toString(2));
                    }
                    else if (result.charAt(0) == '{') {
                        JSONObject jsonObject = new JSONObject(result);
                        Log.d(TAG, "onPostExecute: "+jsonObject.toString(2));
                    } else {
                        System.out.println(result);
                    }
                }
                jsonArray = new JSONArray(result);
                HappyScore = jsonArray.getJSONObject(0).getJSONObject("faceAttributes").getJSONObject("emotion").getDouble("happiness");
                String HappyscoreString = "Happiness value: " + HappyScore*100;
                Log.d(TAG, "onPostExecute: "+ HappyscoreString);

            }catch (JSONException e){
                Log.e(TAG, "onPostExecute: "+e.getMessage() );
            }
        }
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
        @Override
        protected String doInBackground(Void... voids) {
            HttpClient httpClient = HttpClients.createDefault();
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            try {
                URIBuilder builder = new URIBuilder(url);
                builder.addParameter("returnFaceAttributes", "emotion,gender,age");

                URI uri = builder.build();
                HttpPost request = new HttpPost(uri);
                Log.d(TAG, "doInBackground: setting request");
                request.setHeader("Content-Type","application/octet-stream");
                request.setHeader("Ocp-Apim-Subscription-Key", "085bc61e9fc744298c0ddb4f9c3b758e");
                Log.d(TAG, "doInBackground: setting header");
                FileEntity fileEntity = new FileEntity(file, "application/octet-stream");
                Log.d(TAG, "doInBackground: converting bmp to byteArray & setting entity " +file.getPath());
                request.setEntity(fileEntity);
                HttpResponse response = httpClient.execute(request);
                Log.d(TAG, "doInBackground: getting response");
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity);
                return  result;

            }catch (Exception e){
                Log.d(TAG, "doInBackground: "+e.getMessage());
            }
            return null;
        }
    }
}

