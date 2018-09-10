package com.gate6.facedetectionlibrary.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.gate6.facedetectionlibrary.R;
import com.gate6.facedetectionlibrary.face.FaceDetectorWrapper;
import com.gate6.facedetectionlibrary.model.AppBeanData;
import com.gate6.facedetectionlibrary.model.ImageUploadModel;
import com.gate6.facedetectionlibrary.ui.camera.CameraSourcePreview;
import com.gate6.facedetectionlibrary.ui.camera.GraphicOverlay;
import com.gate6.facedetectionlibrary.utils.FaceDetectedListner;
import com.gate6.facedetectionlibrary.utilsPkg.Communicator;
import com.gate6.facedetectionlibrary.utilsPkg.Constant;
import com.gate6.facedetectionlibrary.utilsPkg.NetworkListiners;
import com.gate6.facedetectionlibrary.utilsPkg.RequestType;
import com.gate6.facedetectionlibrary.utilsPkg.Utils;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.face.Face;

import java.util.HashMap;
import java.util.Locale;

public class FaceDetectionActivity extends AppCompatActivity implements NetworkListiners, LocationListener, TextToSpeech.OnInitListener, FaceDetectedListner {
    private static final String TAG = "Face Detector";
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private Context mContext;
    private static final int RC_GET_LOCATION = 4;
    Handler handler;
    private ImageUploadModel imageModel;
    private LocationManager locationManager;
    private double latitude, longitude;
    private TextToSpeech tts;
    private boolean isLogin = false;
    private boolean isFromLogin = true;
    private int DETECTING_TIMER = 5 * 1000;
    private int SPEAKING_OUT_TIMER = 10 * 1000;


    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);
        mContext = this;
        if (getIntent() != null) {
            isFromLogin = getIntent().getBooleanExtra("IS_FROM_LOGIN", true);
        }
        tts = new TextToSpeech(mContext, this);
        handler = new Handler();
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        initializeFaceDetector();
        FaceDetectorWrapper.getInstance().init(mContext, mGraphicOverlay, mPreview, this, DETECTING_TIMER, SPEAKING_OUT_TIMER);
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            openCameraPreview();
        } else {
            requestCameraPermission();
        }
        getLocation();
    }

    private void initializeFaceDetector() {
        /**
         * mContext :  Context
         * mGraphicOverlay :  instance mGraphicOverlay
         * mPreview :  instance CameraSourcePreview
         * this :  callback listner
         * DETECTING_TIMER : interval timer for continuous face detection
         * SPEAKING_OUT_TIMER : interval timer for detecting user
         */
        FaceDetectorWrapper.getInstance().init(mContext, mGraphicOverlay, mPreview, this, DETECTING_TIMER, SPEAKING_OUT_TIMER);
    }

    public void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, RC_GET_LOCATION);
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }


    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        FaceDetectorWrapper.getInstance().startCameraSource(mContext);
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        FaceDetectorWrapper.getInstance().stopPreview();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        FaceDetectorWrapper.getInstance().onDestroyCamera();
        //Close the Text to Speech Library
        if (tts != null) {

            tts.stop();
            tts.shutdown();
            Log.d(TAG, "TTS Destroyed");
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case RC_HANDLE_CAMERA_PERM:
                if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Camera permission granted - initialize the camera source");
                    // we have permission, so create the camerasource
                    openCameraPreview();
                    getLocationPermission();
                    return;
                } else {
                    showPermissionFailedDialog(getString(R.string.no_camera_permission));
                }
                break;
            case RC_GET_LOCATION:
                if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation();
                    return;
                } else {
                    showPermissionFailedDialog(getString(R.string.no_location_permission));
                }
                break;
        }


    }

    private void openCameraPreview() {
        FaceDetectorWrapper.getInstance().createCameraSource();
    }

    private void showPermissionFailedDialog(String message) {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pemission Error")
                .setMessage(message)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }


    private void sendImageToServer(byte[] bmp, String fileName) {
        HashMap<String, String> hmap = new HashMap<String, String>();
        hmap.put("fileName", fileName);
        hmap.put("imageType", "image/jpeg");
        hmap.put("latitude", "" + latitude);
        hmap.put("longitude", "" + longitude);
        if (isFromLogin) {
            Communicator.getInstance().sendFileRequestPost(mContext, bmp, fileName, RequestType.LOGIN_REQUEST,
                    Constant.LOGIN_URL, hmap, this);
        } else {
            Communicator.getInstance().sendFileRequestPost(mContext, bmp, fileName, RequestType.LOGOUT_REQUEST,
                    Constant.LOGOUT_URL, hmap, this);
        }

    }

    @Override
    public void onCompleteResponse(int RequestTypee, AppBeanData data) {
        if (RequestTypee == RequestType.LOGIN_REQUEST) {
            imageModel = (ImageUploadModel) data;
            handler.post(updateRunnableLogin);
        } else if (RequestTypee == RequestType.LOGOUT_REQUEST) {
            imageModel = (ImageUploadModel) data;
            handler.post(updateRunnableLogout);
        }

    }

    @Override
    public void onError(VolleyError volleyError) {

    }

    final Runnable updateRunnableLogout = new Runnable() {
        public void run() {
            // call the activity method that updates the UI
            if (imageModel != null) {
                if (imageModel.getStatus() == 200) {
                    if (!isLogin) {
                        if (imageModel.getData() != null && !TextUtils.isEmpty(imageModel.getData().getName())) {
                            if (!imageModel.getData().getName().equalsIgnoreCase("Unknown")) {
                                /*if (!TextUtils.isEmpty(imageModel.getMessage())) {
                                    Utils.getInstance().showToast(mContext, imageModel.getMessage());
                                }*/
                                isLogin = true;
                                startDashBoardActivity();
                                FaceDetectorWrapper.getInstance().stopAllTimers();
                            } else {
                                if (!TextUtils.isEmpty(imageModel.getMessage())) {
                                    Utils.getInstance().showToast(mContext, imageModel.getMessage());
                                }
                                FaceDetectorWrapper.getInstance().startPreviewAgain();
                            }
                        } else {
                            if (!TextUtils.isEmpty(imageModel.getMessage())) {
                                Utils.getInstance().showToast(mContext, imageModel.getMessage());
                            }
                            FaceDetectorWrapper.getInstance().startPreviewAgain();
                        }
                    }
                }
            }
        }
    };


    final Runnable updateRunnableLogin = new Runnable() {
        public void run() {
            // call the activity method that updates the UI
            if (imageModel != null) {
                if (imageModel.getStatus() == 200) {
                    if (!isLogin) {
                        if (imageModel.getData() != null && !TextUtils.isEmpty(imageModel.getData().getName())) {
                            if (!imageModel.getData().getName().equalsIgnoreCase("Unknown")) {
                                if (!TextUtils.isEmpty(imageModel.getData().getFirstname()) && !TextUtils.isEmpty(imageModel.getData().getLastname())) {
                                    isLogin = true;
                                    startDashBoardActivity();
//                                    speakOut("Welcome " + imageModel.getData().getFirstname() + " " + imageModel.getData().getLastname());
//                                    showAlertDialog("Welcome " + imageModel.getData().getFirstname() + " " + imageModel.getData().getLastname());
                                    FaceDetectorWrapper.getInstance().stopAllTimers();
                                }
                            } else {
                                if (!TextUtils.isEmpty(imageModel.getMessage())) {
                                    Utils.getInstance().showToast(mContext, imageModel.getMessage());
                                }
                                FaceDetectorWrapper.getInstance().startPreviewAgain();
                            }
                        } else {
                            if (!TextUtils.isEmpty(imageModel.getMessage())) {
                                Utils.getInstance().showToast(mContext, imageModel.getMessage());
                            }
                            FaceDetectorWrapper.getInstance().startPreviewAgain();
                        }
                    }
                }
            }
        }
    };


    private void showAlertDialog(String message) {
        android.support.v7.app.AlertDialog.Builder builder1 = new android.support.v7.app.AlertDialog.Builder(mContext);
        builder1.setMessage(message);
        builder1.setCancelable(false);

        builder1.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        startDashBoardActivity();
                    }
                });

        android.support.v7.app.AlertDialog alert11 = builder1.create();
        alert11.show();
    }


    private void startDashBoardActivity() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("DATA_MODEL", imageModel);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();


    }


    void getLocation() {
        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, this);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Please Enable GPS and Internet", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }


    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
        } else {
            Log.e("TTS", "Initilization Failed!");
        }

    }


    private void speakOut(String message) {
        tts.setPitch(0.6f);
        tts.setSpeechRate(2);
        if (tts != null && !tts.isSpeaking()) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }
    }


    @Override
    public void onMissing(Detector.Detections<Face> detectionResults) {
        Log.v(TAG, "face removed");
    }

    @Override
    public void onImageSucces(byte[] result) {
        if (result != null) {
            sendImageToServer(result, "image_face");
        }
    }

    @Override
    public void onSpeak(String message) {
        speakOut(message);
    }

    @Override
    public void cancellAllRequest() {
        Communicator.getInstance().cancelAllRequestQueue();
    }
}