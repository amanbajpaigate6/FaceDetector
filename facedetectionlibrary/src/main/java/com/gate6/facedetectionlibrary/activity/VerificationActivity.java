package com.gate6.facedetectionlibrary.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;

import com.android.volley.VolleyError;
import com.gate6.facedetectionlibrary.R;
import com.gate6.facedetectionlibrary.model.AppBeanData;
import com.gate6.facedetectionlibrary.model.ImageUploadModel;
import com.gate6.facedetectionlibrary.utilsPkg.Communicator;
import com.gate6.facedetectionlibrary.utilsPkg.Constant;
import com.gate6.facedetectionlibrary.utilsPkg.NetworkListiners;
import com.gate6.facedetectionlibrary.utilsPkg.RequestType;
import com.gate6.facedetectionlibrary.utilsPkg.Utils;

import org.json.JSONObject;

public class VerificationActivity extends AppCompatActivity implements NetworkListiners {

    private TextInputLayout verificationTextInput;
    private ProgressDialog progressDialog;
    private Handler handler;
    private Context mContext;
    private ImageUploadModel imageModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);
        initViews();
    }

    private void initViews() {
        mContext = this;
        handler = new Handler();
        verificationTextInput = (TextInputLayout) findViewById(R.id.verificationTextInput);
        findViewById(R.id.verify_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callVerificationApi();
            }
        });
    }

    private void callVerificationApi() {
        if (TextUtils.isEmpty(verificationTextInput.getEditText().getText().toString())) {
            verificationTextInput.setError(getString(R.string.please_enter_verification_code));
        } else {
            progressDialog = ProgressDialog.show(this, "", "Please Wait...", true);
            JSONObject hmap = new JSONObject();
            try {
                hmap.put("verificationId", verificationTextInput.getEditText().getText().toString().trim());
            } catch (Exception e) {
                e.printStackTrace();
            }
            Communicator.getInstance().addJsonRequestPost(mContext, RequestType.VERIFY_REQUEST, Constant.VERIFY_URL, hmap, this, true);

        }
    }

    @Override
    public void onCompleteResponse(int RequestTypee, AppBeanData data) {
        if (RequestTypee == RequestType.VERIFY_REQUEST) {
            imageModel = (ImageUploadModel) data;
            handler.post(updateRunnableUpload);
        }
    }

    @Override
    public void onError(VolleyError volleyError) {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    final Runnable updateRunnableUpload = new Runnable() {
        public void run() {
            // call the activity method that updates the UI
            if (imageModel != null) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                if (imageModel.getStatus() == 200) {
                    if (imageModel.getData() != null) {
                        if (!TextUtils.isEmpty(imageModel.getMessage())) {
                            showAlertDialog(imageModel.getMessage());
                        }
                    } else {
                        if (!TextUtils.isEmpty(imageModel.getMessage())) {
                            Utils.getInstance().showToast(mContext, imageModel.getMessage());
                        }
                    }
                } else {
                    if (!TextUtils.isEmpty(imageModel.getMessage())) {
                        Utils.getInstance().showToast(mContext, imageModel.getMessage());
                    }
                }
            }
        }
    };

    private void showAlertDialog(String message) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(mContext);
        builder1.setMessage(message);
        builder1.setCancelable(false);

        builder1.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        startSplashActivity();
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    private void startSplashActivity() {
        try {
            Intent intent = new Intent(this, ImagePickerActivity.class);
            intent.putExtra("USERID", imageModel.getData().getUser_id());
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
