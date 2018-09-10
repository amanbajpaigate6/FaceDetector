package com.gate6.facedetectionlibrary.face;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.gate6.facedetectionlibrary.R;
import com.gate6.facedetectionlibrary.ui.camera.CameraSourcePreview;
import com.gate6.facedetectionlibrary.ui.camera.GraphicOverlay;
import com.gate6.facedetectionlibrary.utils.ConfigData;
import com.gate6.facedetectionlibrary.utils.FaceDetectedListner;
import com.gate6.facedetectionlibrary.utils.FaceDetectorCallback;
import com.gate6.facedetectionlibrary.utilsPkg.Constant;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class FaceDetectorWrapper implements FaceDetectorCallback {

    public static FaceDetectorWrapper instance;
    private int facing;
    private Context mContext;
    private GraphicOverlay mGraphicOverlay;
    private FaceDetectedListner listner;
    private static final int RC_HANDLE_GMS = 9001;
    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private static final String TAG = "Face Detector";
    private boolean isDistanceNear = false, savedStreamMuted;
    private boolean isFirstTime = true;
    private int xOffset;
    Handler handler;
    private Timer timer;
    private TimerTask timerTask;
    private AudioManager mgr;
    private Timer timerSpeakOut = null;
    private TimerTask timerSpeakOutTask;
    private int DETECTING_TIMER = 5 * 1000;
    private int SPEAKING_OUT_TIMER = 10 * 1000;
    public String frameColor = "#0000FF";
    public boolean isFrameShowing;
    public String frameLabel = "DETECTING";
    public boolean isFrameLabelShowing;
    public boolean isVoiceNeeded;

    public static FaceDetectorWrapper getInstance() {
        if (instance == null) {
            instance = new FaceDetectorWrapper();
        }
        return instance;
    }

    public void setConfigData(ConfigData data) {
        if (data.getLoginUrl() != null && !TextUtils.isEmpty(data.getLoginUrl())) {
            Constant.LOGIN_URL = data.getLoginUrl();
        }
        if (data.getLogoutUrl() != null && !TextUtils.isEmpty(data.getLogoutUrl())) {
            Constant.LOGOUT_URL = data.getLogoutUrl();
        }
        if (data.getSignUpUrl() != null && !TextUtils.isEmpty(data.getSignUpUrl())) {
            Constant.SIGNUP_URL = data.getSignUpUrl();
        }
        if (data.getVerifyUserUrl() != null && !TextUtils.isEmpty(data.getVerifyUserUrl())) {
            Constant.VERIFY_URL = data.getVerifyUserUrl();
        }
        if (data.getVerifyImageUrl() != null && !TextUtils.isEmpty(data.getVerifyImageUrl())) {
            Constant.VERIFY__IMAGE_URL = data.getVerifyImageUrl();
        }

        if (data.getFrameColor() != null && !TextUtils.isEmpty(data.getFrameColor())) {
            frameColor = data.getFrameColor();
        }

        isFrameShowing = data.isFrameShowing();
        isFrameLabelShowing = data.isFrameLabelShowing();
        isVoiceNeeded = data.isVoiceNeeded();

        if (data.getFrameLabel() != null && !TextUtils.isEmpty(data.getFrameLabel())) {
            frameLabel = data.getFrameLabel();
        }

        DETECTING_TIMER = data.getDetectionTimerInterval();
    }

    public void init(Context context, GraphicOverlay mGraphicOverlay, CameraSourcePreview mPreview, FaceDetectedListner listner, int detecting_timer, int speaking_out_timer) {
        mContext = context;
        this.mGraphicOverlay = mGraphicOverlay;
        this.listner = listner;
        this.mPreview = mPreview;
        handler = new Handler();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     * mContext :  Context
     * mGraphicOverlay :  instance mGraphicOverlay
     * mPreview :  instance CameraSourcePreview
     * this :  callback listner
     */
    public void createCameraSource() {
        FaceDetector detector = new FaceDetector.Builder(mContext)
                .setClassificationType(FaceDetector.ACCURATE_MODE)
//                .setProminentFaceOnly(true)
//                .setTrackingEnabled(false)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
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
            Log.w("TAG", "Face detector dependencies are not yet available.");
        }

        try {
            boolean mIsFrontFacing = checkCameraFront(mContext);
            facing = CameraSource.CAMERA_FACING_FRONT;
            if (!mIsFrontFacing) {
                facing = CameraSource.CAMERA_FACING_BACK;
            } else {
                facing = CameraSource.CAMERA_FACING_FRONT;
            }
        } catch (Exception e) {
            e.printStackTrace();
            facing = CameraSource.CAMERA_FACING_BACK;
        }

        mCameraSource = new CameraSource.Builder(mContext, detector)
                .setRequestedPreviewSize(320, 240)
//                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setFacing(facing)
                .setRequestedFps(30.0f)
                .build();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     *
     * @param mContext
     */
    public void startCameraSource(Context mContext) {
        try {
            // check that the device has play services available.
            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
            int code = googleApiAvailability.isGooglePlayServicesAvailable(mContext);
            if (code != ConnectionResult.SUCCESS) {
                Dialog dlg =
                        GoogleApiAvailability.getInstance().getErrorDialog(((Activity) this.mContext), code, RC_HANDLE_GMS);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean checkCameraFront(Context context) {
        try {
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
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
            return new GraphicFaceTracker(mGraphicOverlay);
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
            mFaceGraphic = new FaceGraphic(overlay, mContext, isFrameLabelShowing, FaceDetectorWrapper.this, frameColor, frameLabel, isFrameShowing);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face, "");
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
            if (listner != null) {
                listner.onMissing(detectionResults);
            }
//            Log.v(TAG, "face removed");
            stopAllTimers();
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }


    public void stopPreview() {
        try {
            if (mPreview != null) {
                mPreview.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDestroyCamera() {
        try {
            if (mCameraSource != null) {
                mCameraSource.release();
            }
            stopAllTimers();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startPreviewAgain() {
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
    public void onFaceDetectedOffset(int distance) {
        if (distance > 80) {
            this.xOffset = distance;
            if (isFirstTime) {
                isFirstTime = false;
                isDistanceNear = true;
                startTimer();
            }
        } else {
            isDistanceNear = false;
            speakOuttimer();
        }
    }

    //To start timer
    private void startTimer() {
        takeImage();
        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        if (xOffset > 80) {
                            if (xOffset < 150) {
                                isDistanceNear = false;
                                speakOuttimer();
                            } else {
                                isDistanceNear = true;
                            }

                            takeImage();
                        }
                    }
                });
            }
        };
        timer.schedule(timerTask, 10, DETECTING_TIMER);

    }

    //To stop timer
    public void stopTimer() {
        try {
            if (timer != null) {
                timer.cancel();
                timer.purge();
            }
            isFirstTime = true;
//            Communicator.getInstance().cancelAllRequestQueue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void takeImage() {
        try {
            mgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            final int stream = AudioManager.STREAM_SYSTEM;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!mgr.isStreamMute(stream)) {
                    savedStreamMuted = true;
                    mgr.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0);
                }
            } else {
                mgr.setStreamMute(stream, true);
            }


            final Handler handler = new Handler();
            Timer t = new Timer();
            t.schedule(new TimerTask() {
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (savedStreamMuted) {
                                    mgr.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0);
                                    savedStreamMuted = false;
                                }
                            } else {
                                mgr.setStreamMute(stream, false);
                            }
                        }
                    });
                }
            }, 1000);


            mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] bytes) {
                    if (isDistanceNear && isVoiceNeeded) {
                        listner.onSpeak(mContext.getString(R.string.detecting_you));
                    }
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

    private void doCrop(byte[] picUri) {
        new UploadFilesTask().execute(picUri);
    }


    public class UploadFilesTask extends AsyncTask<byte[], Integer, byte[]> {
        protected byte[] doInBackground(byte[]... data) {
            Bitmap bitmap = null;
            byte[] byteArray = null;
            bitmap = getFace(data[0]);
            if (bitmap != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byteArray = stream.toByteArray();
                bitmap.recycle();
            }

            return byteArray;
        }


        protected void onPostExecute(byte[] result) {
            if (result != null && result.length > 0) {
                Log.v(TAG, "image send success");
                try {
                    mPreview.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                listner.onImageSucces(result);
            } else {
                FaceDetectorWrapper.getInstance().startPreviewAgain();
                Log.v(TAG, "image failed");

            }
        }
    }

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

            if (faces != null && faces.size() > 0) {
            } else {
                faceDetector = new FaceDetector.Builder(mContext)
                        .build();
                frame = new Frame.Builder().setBitmap(bitmap).build();
                faces = faceDetector.detect(frame);
            }
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

    private void speakOuttimer() {
        {
            if (isVoiceNeeded) {
                if (timerSpeakOut == null) {
                    timerSpeakOut = new Timer();
                    timerSpeakOutTask = new TimerTask() {
                        public void run() {
                            handler.post(new Runnable() {
                                public void run() {
                                    if (!isDistanceNear) {
                                        listner.onSpeak(mContext.getString(R.string.come_closer));
                                    }

                                }
                            });
                        }
                    };
                    timerSpeakOut.schedule(timerSpeakOutTask, 10, SPEAKING_OUT_TIMER);
                }
            }
        }
    }

    //To stop timer
    private void stopSpeakOutTimer() {
        try {
            if (timerSpeakOut != null) {
                timerSpeakOut.cancel();
                timerSpeakOut.purge();
                timerSpeakOut = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopAllTimers() {
        stopTimer();
        stopSpeakOutTimer();
        if (listner != null) {
            listner.cancellAllRequest();
        }
    }
}
