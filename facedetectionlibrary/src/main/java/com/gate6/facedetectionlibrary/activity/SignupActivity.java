package com.gate6.facedetectionlibrary.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.Spinner;

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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class SignupActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener, NetworkListiners {


    private TextInputLayout firstNameTextInput, lastNameTextInput, emailTextInput, phoneTextInput;
    private Spinner selectRoles;
    private int REQ_IMAGE_PICK = 1;
    private byte[] imageByteArray;
    ScrollView mainContainer;
    FrameLayout image_container;
    private String emailValue, firstNameValue, lastNameValue, rolesValue, phoneValue;
    private Context mContext;
    private ProgressDialog progressDialog;
    private String blockCharacterSet = "~#^|$%&*!";
    private ImageUploadModel imageModel;
    private Handler handler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        initViews();
    }

    private void initViews() {
        handler = new Handler();
        mContext = this;
        image_container = (FrameLayout) findViewById(R.id.image_container);
        mainContainer = (ScrollView) findViewById(R.id.mainContainer);
        image_container.setVisibility(View.GONE);
        firstNameTextInput = (TextInputLayout) findViewById(R.id.firstNameTextInput);
        lastNameTextInput = (TextInputLayout) findViewById(R.id.lastNameTextInput);
        emailTextInput = (TextInputLayout) findViewById(R.id.emailTextInput);
        phoneTextInput = (TextInputLayout) findViewById(R.id.phoneTextInput);
        selectRoles = (Spinner) findViewById(R.id.selectRoles);
        firstNameTextInput.getEditText().setFilters(new InputFilter[]{filter});
        lastNameTextInput.getEditText().setFilters(new InputFilter[]{filter});
        setSpinnerDropdown();
        findViewById(R.id.signup_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUpClick();
            }
        });
    }

    private void setSpinnerDropdown() {
        //get the spinner from the xml.
        selectRoles.setOnItemSelectedListener(this);
        //create a list of items for the spinner.
        String[] items = new String[]{"Patient", "Doctor"};
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        //set the spinners adapter to the previously created one.
        selectRoles.setAdapter(adapter);
    }


    private InputFilter filter = new InputFilter() {

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (source != null && blockCharacterSet.contains(("" + source))) {
                return "";
            }
            return null;
        }
    };

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onClick(View v) {
        /*switch (v.getId()) {
            case R.id.signup_button:
                signUpClick();
                break;
        }*/
    }

    private void signUpClick() {
        emailValue = emailTextInput.getEditText().getText().toString();
        firstNameValue = firstNameTextInput.getEditText().getText().toString();
        lastNameValue = lastNameTextInput.getEditText().getText().toString();
        rolesValue = selectRoles.getSelectedItem().toString();
        phoneValue = phoneTextInput.getEditText().getText().toString();

        if (TextUtils.isEmpty(firstNameValue)) {
            firstNameTextInput.setError(getString(R.string.please_enter_first_name));
        } else if (TextUtils.isEmpty(lastNameValue)) {
            lastNameTextInput.setError(getString(R.string.please_enter_last_name));
            firstNameTextInput.setError(null);
        } else if (TextUtils.isEmpty(emailValue)) {
            emailTextInput.setError(getString(R.string.please_enter_email));
            firstNameTextInput.setError(null);
            lastNameTextInput.setError(null);
        } else if (!Utils.getInstance().isEmailValid(emailValue)) {
            emailTextInput.setError(getString(R.string.please_enter_valid_email));
            firstNameTextInput.setError(null);
            lastNameTextInput.setError(null);
        } else if (TextUtils.isEmpty(phoneValue)) {
            phoneTextInput.setError(getString(R.string.please_enter_phone_number));
            firstNameTextInput.setError(null);
            lastNameTextInput.setError(null);
            emailTextInput.setError(null);
        } else if (TextUtils.isEmpty(rolesValue)) {
            Utils.getInstance().showToast(mContext, getString(R.string.select_roles));
        } else {
            firstNameTextInput.setError(null);
            lastNameTextInput.setError(null);
            phoneTextInput.setError(null);
            emailTextInput.setError(null);
            callServerApi();
        }
    }

    private void callServerApi() {
        progressDialog = ProgressDialog.show(this, "", "Please Wait...", true);
        JSONObject hmap = new JSONObject();
        try {
            hmap.put("userEmail", emailValue);
            hmap.put("userFirstname", firstNameValue);
            hmap.put("userLastname", lastNameValue);
            hmap.put("userRole", rolesValue);
            hmap.put("userPhoneNumber", phoneValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Communicator.getInstance().addJsonRequestPost(mContext, RequestType.REGISTER_REQUEST, Constant.SIGNUP_URL, hmap, this, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_IMAGE_PICK) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Uri myUri = Uri.parse(data.getExtras().getString("IMAGE"));
                    if (myUri != null) {
                        InputStream iStream = null;
                        try {
                            iStream = getContentResolver().openInputStream(myUri);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        imageByteArray = getBytes(iStream);
                    }
                }
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


    public void setImageData(byte[] byteArray) {
        try {
            mainContainer.setVisibility(View.VISIBLE);
            image_container.setVisibility(View.GONE);
            this.imageByteArray = byteArray;
            Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            if (bmp != null) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCompleteResponse(int RequestTypee, AppBeanData data) {
        if (RequestTypee == RequestType.REGISTER_REQUEST) {
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
                    if(imageModel.getData()!=null) {
                        if (!TextUtils.isEmpty(imageModel.getMessage())) {
//                        Utils.getInstance().showToast(mContext, imageModel.getMessage());
                            showAlertDialog(imageModel.getMessage());
                        }
                    }else{
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
            intent.putExtra("VERIFICATION_CODE", imageModel.getData().getVerification_code());
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        return super.dispatchTouchEvent(ev);
    }
}
