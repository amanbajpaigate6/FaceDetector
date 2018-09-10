package com.gate6.facedetectionlibrary.utils;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

public interface FaceDetectedListner {

//    public void onNewItem(int faceId, Face item) ;
//
//    public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) ;

    public void onMissing(FaceDetector.Detections<Face> detectionResults) ;

    public void onImageSucces(byte[] result);

    public void onSpeak(String message);

    public void cancellAllRequest();


//    public void onDone();
}
