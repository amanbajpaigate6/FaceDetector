package com.gate6.facedetectionlibrary.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.gate6.facedetectionlibrary.R;
import com.gate6.facedetectionlibrary.face.FaceDetectorWrapper;
import com.gate6.facedetectionlibrary.face.FaceGraphic;
import com.gate6.facedetectionlibrary.model.AppBeanData;
import com.gate6.facedetectionlibrary.model.ImageUploadModel;
import com.gate6.facedetectionlibrary.ui.camera.CameraSourcePreview;
import com.gate6.facedetectionlibrary.ui.camera.GraphicOverlay;
import com.gate6.facedetectionlibrary.utils.FaceDetectedListner;
import com.gate6.facedetectionlibrary.utils.FaceDetectorCallback;
import com.gate6.facedetectionlibrary.utilsPkg.Communicator;
import com.gate6.facedetectionlibrary.utilsPkg.Constant;
import com.gate6.facedetectionlibrary.utilsPkg.NetworkListiners;
import com.gate6.facedetectionlibrary.utilsPkg.RequestType;
import com.gate6.facedetectionlibrary.utilsPkg.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class ImagePickerActivity extends AppCompatActivity implements TextToSpeech.OnInitListener, NetworkListiners,FaceDetectedListner, FaceDetectorCallback {

    private static final String TAG = "Face Dectector";

    private CameraSource mCameraSource = null;

    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private Context mContext;
    private static final int RC_READ_WRITE_STORAGE = 3;
    Handler handler;
    private ImageUploadModel imageModel;
    private Timer timer;
    private TimerTask timerTask;
    private AudioManager mgr;
    private TextToSpeech tts;
    private ProgressDialog progressDialog;
    //    private ImageUploadModel modelData;
    private String user_id = "", verification_code = "";
    private TextView verificatioCodeText;
    private LinearLayout headerLayout;
    private ImageView userImage;
    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_image_picker);
        mContext = this;
        handler = new Handler();
        tts = new TextToSpeech(mContext, this);
        verificatioCodeText = (TextView) findViewById(R.id.verificatioCode);
        headerLayout = (LinearLayout) findViewById(R.id.headerLayout);
        if (getIntent() != null) {
//            modelData= (ImageUploadModel) getIntent().getSerializableExtra("VERIFICATION_MODEL");
            user_id = getIntent().getStringExtra("USERID");
            verification_code = getIntent().getStringExtra("VERIFICATION_CODE");
        }

        if (!TextUtils.isEmpty(verification_code)) {
            headerLayout.setVisibility(View.VISIBLE);
            verificatioCodeText.setText(verification_code);
        } else {
            headerLayout.setVisibility(View.GONE);
        }
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
//        findViewById(R.id.takePhoto).setVisibility(View.VISIBLE);
        findViewById(R.id.takePhoto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeImage();
//                isReadStoragePermissionGranted();
            }
        });
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            isReadStoragePermissionGranted();
            createCameraSource();
        } else {
            requestCameraPermission();
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

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
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new ImagePickerActivity.GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
//                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case RC_READ_WRITE_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("value", "Permission Granted, Now you can use local drive .");
                    return;
                } else {
                    showPermissionFailedDialog(getString(R.string.no_write_permission));
                }
                break;

            case RC_HANDLE_CAMERA_PERM:
                if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Camera permission granted - initialize the camera source");
                    // we have permission, so create the camerasource
                    createCameraSource();
                    return;
                }else{
                    showPermissionFailedDialog(getString(R.string.no_camera_permission));
                }
                break;


        }


      /*  if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }*/


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

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    @Override
    public void onMissing(Detector.Detections<Face> detectionResults) {

    }

    @Override
    public void cancellAllRequest() {

    }

    @Override
    public void onImageSucces(byte[] result) {

    }

    @Override
    public void onSpeak(String message) {

    }

    @Override
    public void onFaceDetectedOffset(int offset) {

    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new ImagePickerActivity.GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay, mContext, FaceDetectorWrapper.getInstance().isFrameLabelShowing, ImagePickerActivity.this, FaceDetectorWrapper.getInstance().frameColor,
                    FaceDetectorWrapper.getInstance().frameLabel,FaceDetectorWrapper.getInstance().isFrameShowing);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            Log.v("aman1", "onNewItem");
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            Log.v("aman1", "onUpdate");

            mOverlay.add(mFaceGraphic);
            if (imageModel != null && imageModel.getData() != null && imageModel.getData().getName() != null && !TextUtils.isEmpty(imageModel.getData().getName())) {
                mFaceGraphic.updateFace(face, imageModel.getData().getName());
            } else {
                mFaceGraphic.updateFace(face, "");
            }
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            Log.v("aman1", "onMissing");
            mOverlay.remove(mFaceGraphic);
            Log.v("aman", "face removed");
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            Log.v("aman1", "onDone");
            mOverlay.remove(mFaceGraphic);
        }
    }


    public boolean isReadStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted1");
                return true;
            } else {

                Log.v(TAG, "Permission is revoked1");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RC_READ_WRITE_STORAGE);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted1");

            return true;
        }
    }

    public Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            cursor.close();
            return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    public File saveImage(byte[] content, String extension) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = new File(storageDir, timeStamp + "." + extension);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(image);
            out.write(content);
            out.close();
        } finally {
            if (out != null) {
                out.close();
            }
        }
        return image;
    }


    public Bitmap scaleDownBitmap(Bitmap photo, int newHeight, Context context) {

        final float densityMultiplier = context.getResources().getDisplayMetrics().density;

        int h = (int) (newHeight * densityMultiplier);
        int w = (int) (h * photo.getWidth() / ((double) photo.getHeight()));

        photo = Bitmap.createScaledBitmap(photo, w, h, true);

        return photo;
    }

    private void takeImage() {
        try {
//            mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//            mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
            mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] bytes) {
//                    mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//                    mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false);
                    mPreview.stop();
                    progressDialog = ProgressDialog.show(mContext, "", "Please Wait...", true);
                    try {
                        doCrop(bytes);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /*private void startDashBoardActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("USER_NAME", imageModel.getData().getName());
        setResult(Activity.RESULT_OK,returnIntent);
        startActivity(intent);
        finish();
    }*/


    public Bitmap getFace(byte[] data) {
        try {

            ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(data);
            Bitmap bitmap = BitmapFactory.decodeStream(arrayInputStream);

            if (bitmap.getWidth() > bitmap.getHeight()) {
                Matrix matrix = new Matrix();
                matrix.postRotate(270f);
                if (bitmap.getWidth() > 1500) {
                    matrix.postScale(0.5f, 0.5f);
                }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            FaceDetector faceDetector = new FaceDetector.Builder(mContext)
                    .setProminentFaceOnly(true)
                    .setTrackingEnabled(false)
                    .build();

            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<Face> faces = faceDetector.detect(frame);
            Bitmap results = null;
            for (int i = 0; i < faces.size(); i++) {
                Face thisFace = faces.valueAt(i);
                float x = thisFace.getPosition().x;
                float y = thisFace.getPosition().y;
                float x2 = (x / 4) + (thisFace.getWidth());
                float y2 = (y / 4) + (thisFace.getHeight());
                results = Bitmap.createBitmap(bitmap, (int) x, (int) y, (int) x2, (int) y2);
            }
            return results;
        } catch (Exception e) {
            Log.e("GET_FACE", " e.message");
        }
        return null;
    }
//    Bitmap bitmap = null;

    private void doCrop(final byte[] picUri) {
        try {
            new UploadFilesTask().execute(picUri);
        }
        // respond to users whose devices do not support the crop action
        catch (Exception e) {
            // display an error message
            e.printStackTrace();
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }


    public class UploadFilesTask extends AsyncTask<byte[], Integer, Uri> {
        protected Uri doInBackground(byte[]... data) {
            Bitmap bitmap = null;
            Uri uri = null;
            byte[] byteArray = null;
            bitmap = getFace(data[0]);
            if (bitmap != null) {
                uri=getImageUri(mContext,bitmap);
//                ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//                byteArray = stream.toByteArray();
                bitmap.recycle();
            }

            return uri;
        }


        protected void onPostExecute(Uri result) {
            if (result != null) {
                Log.v("image", "uri success");
//                sendImageServer(result);
                sendImageUri(result);
            } else {
                Utils.getInstance().showToast(mContext, getString(R.string.unable_to_detect));
            }
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            startCameraSource();
        }
    }

    private void sendImageUri(Uri result) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("IMAGE_URI", result.toString());
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    private void sendImageServer(byte[] imageArray) {
        HashMap<String, String> hmap = new HashMap<String, String>();
        hmap.put("fileName", "image");
        hmap.put("imageType", "image/jpeg");
        hmap.put("user_id", user_id);
//        hmap.put("latitude", "" + latitude);
//        hmap.put("longitude", "" + longitude);

        Communicator.getInstance().sendFileRequestPost(mContext, imageArray, "image", RequestType.VERIFY_IMAGE_REQUEST,
                Constant.VERIFY__IMAGE_URL, hmap, this);

    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
//                btnSpeak.setEnabled(true);
//                speakOut();
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }

    }

    private void speakOut() {

        String text = "please come closer";
        tts.setPitch(0.6f);
        tts.setSpeechRate(1);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public void onCompleteResponse(int RequestTypee, AppBeanData data) {
        if (RequestTypee == RequestType.VERIFY_IMAGE_REQUEST) {
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
                if (imageModel.getStatus() == 200) {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    if (!TextUtils.isEmpty(imageModel.getMessage())) {
                        Utils.getInstance().showToast(mContext, imageModel.getMessage());
                    }
                    startDashBoardActivity();
                }else{
                    if (!TextUtils.isEmpty(imageModel.getMessage())) {
                        Utils.getInstance().showToast(mContext, imageModel.getMessage());
                    }
                }
            }
        }
    };

    private void startDashBoardActivity() {
//        Intent intent = new Intent(this, SplashActivity.class);
//        startActivity(intent);
        finish();
    }


}
