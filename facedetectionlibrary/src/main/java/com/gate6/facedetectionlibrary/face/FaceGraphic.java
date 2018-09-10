/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gate6.facedetectionlibrary.face;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;

import com.gate6.facedetectionlibrary.ui.camera.GraphicOverlay;
import com.gate6.facedetectionlibrary.utils.FaceDetectedListner;
import com.gate6.facedetectionlibrary.utils.FaceDetectorCallback;
import com.google.android.gms.vision.face.Face;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
public class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private static final int COLOR_CHOICES[] = {
            Color.RED,
            /*Color.CYAN,
            Color.GREEN,
            Color.MAGENTA,
            Color.RED,
            Color.WHITE,
            Color.YELLOW*/
    };
    private static int mCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;

    private volatile Face mFace;
    private int mFaceId;
    private float mFaceHappiness;
    private Context mContext;
    private String face_name;
    private FaceDetectorCallback listner;
    private boolean isShowDetecting;
    public boolean isFrameShowing;
    public String frameLabel = "Detecting";

    public FaceGraphic(GraphicOverlay overlay, Context mContext, boolean isShowDetecting, FaceDetectorCallback listner, String frameColor, String frameLabel,
                       boolean isFrameShowing) {
        super(overlay);
        this.mContext = mContext;
        this.listner = listner;

        this.isShowDetecting = isShowDetecting;
        this.isFrameShowing = isFrameShowing;
        this.frameLabel = frameLabel;

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mFacePositionPaint = new Paint();

        mIdPaint = new Paint();
        mIdPaint.setTextSize(ID_TEXT_SIZE);

        mBoxPaint = new Paint();
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);

        try {
            if (!TextUtils.isEmpty(frameColor)) {
                mFacePositionPaint.setColor(Color.parseColor(frameColor));
                mIdPaint.setColor(Color.parseColor(frameColor));
                mBoxPaint.setColor(Color.parseColor(frameColor));
            } else {
                mFacePositionPaint.setColor(selectedColor);
                mIdPaint.setColor(selectedColor);
                mBoxPaint.setColor(selectedColor);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mFacePositionPaint.setColor(selectedColor);
            mIdPaint.setColor(selectedColor);
            mBoxPaint.setColor(selectedColor);
        }
    }

    public void setId(int id) {
        mFaceId = id;
    }


    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    public void updateFace(Face face, String name) {
        mFace = face;
        postInvalidate();
        face_name = name;
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }

        // Draws a circle at the position of the detected face, with the face's track id below.
        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);
        // Draws a bounding box around the face.
        float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);
        float left = (x - xOffset);
        float top = (y - yOffset);
        float right = (x + xOffset);
        float bottom = (y + yOffset) + 50;
//        if (mContext instanceof SigninActivity || mContext instanceof LogoutActivity) {

            if (isShowDetecting) {
                canvas.drawText(frameLabel, left + 20, top - 20, mIdPaint);
            }
        checkDistance(xOffset);
        if(isFrameShowing) {
            canvas.drawRect(left, top, right, bottom, mBoxPaint);
        }
//        canvas.drawRect(left, top, right, bottom, mBoxPaint);
    }

    private void checkDistance(float xOffset) {
        /*if (mContext instanceof SigninActivity) {
            ((SigninActivity) mContext).callImageCapture(xOffset);
        } else if (mContext instanceof LogoutActivity) {
            ((LogoutActivity) mContext).callImageCapture(xOffset);
        }*/
        if (listner != null) {
            listner.onFaceDetectedOffset((int) xOffset);
        }
    }


}
