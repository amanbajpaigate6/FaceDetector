package com.gate6.facedetectionlibrary.utilsPkg;

import com.android.volley.VolleyError;
import com.gate6.facedetectionlibrary.model.AppBeanData;

public interface NetworkListiners {
    void onCompleteResponse(int RequestType, AppBeanData data);

    void onError(VolleyError volleyError);

}