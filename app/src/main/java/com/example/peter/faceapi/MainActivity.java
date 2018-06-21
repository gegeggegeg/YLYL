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
import java.util.ArrayList;
import java.util.List;

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

    //log tag
    private static final String TAG = "MainActivity";
    //UI
    private Button startBtn;

    private Integer permissionRequestCode = 1234;
    private ArrayList<YoutubevideoUnit> playlist = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startBtn = findViewById(R.id.startBtn);
        //Ask for permission
        getPermission();
        //Initialize playlist
        setPlaylist();
        // set Click listener for new intent
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,GameActivity.class);
                Log.d(TAG, "onClick: transfer playlist to GameActivity");
                intent.putExtra("playlist",playlist);
                startActivity(intent);
            }
        });
    }
    private void getPermission(){
        Log.d(TAG, "getPermission: request premission from user");
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE,CAMERA}
                ,permissionRequestCode);
    }

    public void setPlaylist() {
        Log.d(TAG, "setPlaylist: Initialize playlist");
        playlist = new ArrayList<YoutubevideoUnit>();
        playlist.add(new YoutubevideoUnit("PfvSPvKQEeI", 10*1000, 20*1000));
        playlist.add(new YoutubevideoUnit("fSnQJFf27us", 10*1000, 20*1000));
        playlist.add(new YoutubevideoUnit("5LvmhbVxjp4", 10*1000, 20*1000));
    }
}
