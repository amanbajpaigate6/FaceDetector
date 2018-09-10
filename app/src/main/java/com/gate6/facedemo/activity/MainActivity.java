package com.gate6.facedemo.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.gate6.facedemo.R;
import com.gate6.facedetectionlibrary.model.ImageUploadModel;
import com.gate6.facedetectionlibrary.activity.FaceDetectionActivity;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        Intent in = new Intent(this, FaceDetectionActivity.class);
        startActivityForResult(in, 111);
    }


}
