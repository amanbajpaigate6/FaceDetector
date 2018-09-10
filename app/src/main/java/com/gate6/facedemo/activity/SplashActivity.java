package com.gate6.facedemo.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.gate6.facedemo.R;
import com.gate6.facedetectionlibrary.activity.FaceDetectionActivity;
import com.gate6.facedetectionlibrary.activity.ImagePickerActivity;
import com.gate6.facedetectionlibrary.activity.SignupActivity;
import com.gate6.facedetectionlibrary.face.FaceDetectorWrapper;
import com.gate6.facedetectionlibrary.model.ImageUploadModel;
import com.gate6.facedetectionlibrary.utils.ConfigData;
import com.gate6.facedetectionlibrary.utilsPkg.Constant;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class SplashActivity extends AppCompatActivity {
    private ImageUploadModel model;
    private Context mContext;
    private int LOGIN_REQUEST = 111;
    private int LOGOUT_REQUEST = 112;
    private int IMAGE_PICK_REQUEST=113;
    private byte[] imageByteArray;
    private ImageView userImage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        mContext = this;
        userImage=(ImageView)findViewById(R.id.userImage);
        userImage.setVisibility(View.GONE);
//        setConfigData();

        findViewById(R.id.signin_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSigninActivity();
            }
        });

        findViewById(R.id.image_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePickerActivity();
            }
        });
        findViewById(R.id.verify_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openVerificationActivity();
            }
        });
        findViewById(R.id.logout_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLogoutActivity();
            }
        });
    }

    private void openImagePickerActivity() {
        Intent intent = new Intent(this, ImagePickerActivity.class);
        startActivityForResult(intent, IMAGE_PICK_REQUEST);
    }

    private void setConfigData() {
        ConfigData data = new ConfigData();
        data.setLoginUrl(Constant.LOGIN_URL);
        data.setLogoutUrl(Constant.LOGOUT_URL);
        data.setSignUpUrl(Constant.SIGNUP_URL);
        data.setVerifyUserUrl(Constant.VERIFY_URL);
        data.setVerifyImageUrl(Constant.VERIFY__IMAGE_URL);
        data.setFrameColor("#000000");
        data.setFrameLabel("please waiting...");
        data.setFrameShowing(true);
        data.setFrameLabelShowing(false);
        data.setVoiceNeeded(true);
        FaceDetectorWrapper.getInstance().setConfigData(data);

    }

    private void openSignupActivity() {
        Intent intent = new Intent(this, SignupActivity.class);
        startActivity(intent);
    }

    private void openSigninActivity() {
//        Intent intent = new Intent(this, SigninActivity.class);
        Intent intent = new Intent(this, FaceDetectionActivity.class);
        intent.putExtra("IS_FROM_LOGIN", true);
        startActivityForResult(intent, LOGIN_REQUEST);
    }

    private void openVerificationActivity() {
//        Intent intent = new Intent(this, VerificationActivity.class);
//        startActivity(intent);
    }

    private void openLogoutActivity() {
//        Intent intent = new Intent(this, LogoutActivity.class);
        Intent intent = new Intent(this, FaceDetectionActivity.class);
        intent.putExtra("IS_FROM_LOGIN", false);
        startActivityForResult(intent, LOGOUT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    model = (ImageUploadModel) data.getExtras().get("DATA_MODEL");
                    if (model != null) {
                        String message = "Welcome " + model.getData().getFirstname() + " " + model.getData().getLastname();
//                        showAlertDialog("Welcome " + model.getData().getFirstname() + " " + model.getData().getLastname());
                        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else if (requestCode == LOGOUT_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    model = (ImageUploadModel) data.getExtras().get("DATA_MODEL");
                    if (model != null) {
                        Toast.makeText(mContext, model.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                }

            }
        }else if (requestCode == IMAGE_PICK_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Uri myUri = Uri.parse(data.getExtras().getString("IMAGE_URI"));
                    if (myUri != null) {
                        InputStream iStream = null;
                        try {
                            iStream = getContentResolver().openInputStream(myUri);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        imageByteArray = getBytes(iStream);
                        userImage.setVisibility(View.VISIBLE);
                        userImage.setImageURI(myUri);
//                        deleteImageFile(myUri);
                    }
                }
            }
        }
    }

    private void deleteImageFile(Uri myUri) {
        File fdelete = new File(myUri.getPath());
        if (fdelete.exists()) {
            if (fdelete.delete()) {
                System.out.println("file Deleted :" + myUri.toString());
            } else {
                System.out.println("file not Deleted :" + myUri.toString());
            }
        }
    }

    public byte[] getBytes(InputStream inputStream) {
        try {
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            int len = 0;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showAlertDialog(String message) {
        android.support.v7.app.AlertDialog.Builder builder1 = new android.support.v7.app.AlertDialog.Builder(this);
        builder1.setMessage(message);
        builder1.setCancelable(false);

        builder1.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
//                        startDashBoardActivity();
                    }
                });

        android.support.v7.app.AlertDialog alert11 = builder1.create();
        alert11.show();
    }

}
