package com.example.peter.faceapi;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.entity.FileEntity;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private String url = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0/detect";
    private ImageView faceImage;
    private TextView happiness;
    private TextView anger;
    private TextView sadness;
    private TextView neutral;
    private TextView gender;
    private TextView age;
    private Integer permissionRequestCode = 1234;
    private ProgressBar progressBar;
    private Button CameraBtn;
    private Uri ImageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();
        getPermission();
        faceImage = findViewById(R.id.faceImage);
        happiness = findViewById(R.id.happiness);
        sadness = findViewById(R.id.sadness);
        anger = findViewById(R.id.anger);
        progressBar = findViewById(R.id.loadingbar);
        neutral = findViewById(R.id.neutral);
        gender = findViewById(R.id.gender);
        age = findViewById(R.id.age);
        progressBar.setVisibility(View.GONE);
        CameraBtn = findViewById(R.id.cameraBtn);
        CameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                File file = new File("/storage/emulated/0/Pictures/FaceImage.jpg");
                intent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(file));
                ImageUri = FileProvider.getUriForFile(MainActivity.this,"com.example.provider", file);
                startActivityForResult(intent,999);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 999 && resultCode == Activity.RESULT_OK){
            Log.d(TAG, "onActivityResult: requestcode ok");
            if(ImageUri != null) {
                Log.d(TAG, "onActivityResult: Uri != null");
                Uri selectedImage = ImageUri;
                getContentResolver().notifyChange(selectedImage, null);
                Bitmap reducedSizeBitmap = getBitmap(ImageUri.getPath());
                if(reducedSizeBitmap != null)
                    faceImage.setImageBitmap(reducedSizeBitmap);
                OutputStream fOut = null;
                File file = new File("/storage/emulated/0/Pictures/", "compressed.jpg");
                try {
                    fOut = new FileOutputStream(file);
                    reducedSizeBitmap.compress(Bitmap.CompressFormat.JPEG,100,fOut);
                    fOut.close();
                }catch (Exception e){
                    Log.e(TAG, "onActivityResult: "+e.getMessage() );
                }
                FaceAPItask faceAPItask = new FaceAPItask(file);
                faceAPItask.execute();
            }
        }
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

                double y = Math.sqrt(IMAGE_MAX_SIZE
                        / (((double) width) / height))/1.5;
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

    private void getPermission(){
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE,CAMERA},permissionRequestCode);
    }

    class FaceAPItask extends AsyncTask<Void, Void, String> {

        private File file;

        public FaceAPItask(File file) {
            super();
            this.file = file;
        }

        @Override
        protected void onPostExecute(String result) {
            progressBar.setVisibility(View.GONE);
            Double HappyScore = 0.0;
            Double AngerScore = 0.0;
            Double SadScore = 0.0;
            Double NeutralScore =0.0;
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
                AngerScore = jsonArray.getJSONObject(0).getJSONObject("faceAttributes").getJSONObject("emotion").getDouble("anger");
                SadScore = jsonArray.getJSONObject(0).getJSONObject("faceAttributes").getJSONObject("emotion").getDouble("sadness");
                NeutralScore = jsonArray.getJSONObject(0).getJSONObject("faceAttributes").getJSONObject("emotion").getDouble("neutral");
                String emotionScore = "Happiness value: " + HappyScore*100;
                String angerString = "Anger value: " + AngerScore*100;
                String sadString = "Sadness value: " + SadScore*100;
                String neutralString = "Neutral value: "+ NeutralScore*100;
                String genderString = "Gender: " + jsonArray.getJSONObject(0).getJSONObject("faceAttributes").getString("gender");
                String ageString = "Age: "+ jsonArray.getJSONObject(0).getJSONObject("faceAttributes").getString("age");
                Log.d(TAG, "onPostExecute: "+emotionScore);
                happiness.setText(emotionScore);
                anger.setText(angerString);
                sadness.setText(sadString);
                neutral.setText(neutralString);
                gender.setText(genderString);
                age.setText(ageString);

            }catch (JSONException e){
                Log.e(TAG, "onPostExecute: "+e.getMessage() );
            }
        }
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            progressBar.setVisibility(View.VISIBLE);
        }
        @Override
        protected String doInBackground(Void... voids) {
            progressBar.setVisibility(View.VISIBLE);
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
