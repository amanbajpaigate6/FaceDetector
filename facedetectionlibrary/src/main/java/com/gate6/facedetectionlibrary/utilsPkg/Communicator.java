package com.gate6.facedetectionlibrary.utilsPkg;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.gate6.facedetectionlibrary.R;
import com.gate6.facedetectionlibrary.model.AppBeanData;
import com.gate6.facedetectionlibrary.model.ImageUploadModel;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Communicator {

    private static Communicator instance;

    public static Communicator getInstance() {
        if (instance == null) {
            instance = new Communicator();
        }
        return instance;
    }

    private Context mContext;
    private String url;
    private int requestType = 0;
    NetworkListiners listiners;


    HashMap<String, String> bundle;
    JSONObject bundleJson;


    public void sendFileRequestPost(Context mContext, final byte[] bmp, final String title, final int requestType, String url, final HashMap<String, String> bundle,
                                    final NetworkListiners listiners) {
        this.requestType = requestType;
        this.url = url;
        this.listiners = listiners;
        this.bundle = bundle;
        Log.v("url", "url is " + url);
        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, url, new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response1) {

                String resultResponse = new String(response1.data);
                {
                    AppBeanData data = null;
                    Gson gson = new Gson();
                    Log.v("aman", "response is " + resultResponse);
                    try {
                        if (listiners != null) {
                            if (requestType == RequestType.LOGIN_REQUEST) {
                                data = gson.fromJson(resultResponse, ImageUploadModel.class);
                            } else if (requestType == RequestType.REGISTER_REQUEST) {
                                data = gson.fromJson(resultResponse, ImageUploadModel.class);
                            } else if (requestType == RequestType.LOGOUT_REQUEST) {
                                data = gson.fromJson(resultResponse, ImageUploadModel.class);
                            } else if (requestType == RequestType.VERIFY_IMAGE_REQUEST) {
                                data = gson.fromJson(resultResponse, ImageUploadModel.class);
                            }
                            listiners.onCompleteResponse(requestType, data);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        listiners.onError(null);
                    }

                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                String errorMessage = "Unknown error";
                if (networkResponse == null) {
                    if (error.getClass().equals(TimeoutError.class)) {
                        errorMessage = "Request timeout";
                    } else if (error.getClass().equals(NoConnectionError.class)) {
                        errorMessage = "Failed to connect server";
                    }
                } else {
                    String result = new String(networkResponse.data);
                    try {
                        JSONObject response = new JSONObject(result);
                        String status = response.getString("status");
                        String message = response.getString("message");

                        Log.e("Error Status", status);
                        Log.e("Error Message", message);

                        if (networkResponse.statusCode == 404) {
                            errorMessage = "Resource not found";
                        } else if (networkResponse.statusCode == 401) {
                            errorMessage = message + " Please login again";
                        } else if (networkResponse.statusCode == 400) {
                            errorMessage = message + " Check your inputs";
                        } else if (networkResponse.statusCode == 500) {
                            errorMessage = message + " Something is getting wrong";
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Log.i("Error", errorMessage);
                error.printStackTrace();
                listiners.onError(error);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> param = new HashMap<>();
                param = bundle;

                return param;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
//                if (bundle != null) {
                params.put("imageFile", new DataPart(title + ".png", bmp, "image/jpeg"));
               /* } else {
                    params.put("videoFile", new DataPart(title, bmp, "video/mp4"));
                }*/

                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
//                headers.put("X-API-KEY", NetworkConstants.HEADER_KEY_URL);
                // headers.put("Content-Type", "application/x-www-form-urlencoded");
//                headers.put("TOKEN", AppData.token);
                return headers;
            }
        };
        multipartRequest.setTag("MULTIPART_REQUEST");


        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                60000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        if (ConnectionDetector.checkNetworkAvailablity(mContext)) {
            VolleySingleton.getInstance(mContext).addToRequestQueue(multipartRequest);

        } else {
            if (mContext instanceof Activity) {
//                Utils.getInstance().showNoInternetAlertDialog(mContext, mContext.getResources().getString(R.string.alert_need_internet_connection), "Alert !");
            }
        }
    }

    public void addJsonRequestPost(final Context mContext, final int requestType, String url, final JSONObject bundle, final NetworkListiners listiners, boolean isTokenNeed) {
        this.mContext = mContext;
        this.requestType = requestType;
        this.url = url;
        this.listiners = listiners;
        this.bundleJson = bundle;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, bundle, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.v("aman", "response is " + response.toString());
                AppBeanData data = null;
                Gson gson = new Gson();
                try {
                    if (listiners != null) {
                        if (requestType == RequestType.REGISTER_REQUEST) {
                            data = gson.fromJson(response.toString(), ImageUploadModel.class);
                        }else   if (requestType == RequestType.VERIFY_REQUEST) {
                            data = gson.fromJson(response.toString(), ImageUploadModel.class);
                        }else   if (requestType == RequestType.LOGOUT_QUES_REQUEST) {
                            data = gson.fromJson(response.toString(), ImageUploadModel.class);
                        }

                        listiners.onCompleteResponse(requestType, data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    //   Utils.getInstance().setToastTypefaceforShort(mContext, mContext.getString(R.string.json_error));
                    listiners.onError(null);
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                try {
                    if (listiners != null) {
                        NetworkResponse networkResponse = error.networkResponse;
                        String errorMessage = "Unknown error";
                        if (networkResponse == null) {
                            if (error.getClass().equals(TimeoutError.class)) {
                                errorMessage = "Request timeout";
                            } else if (error.getClass().equals(NoConnectionError.class)) {
                                errorMessage = "Failed to connect server";
                            }
                        } else {
                            String result = new String(networkResponse.data);
                            JSONObject response = new JSONObject(result);
                            String status = response.getString("status");
                            String message = response.getString("message");
                        }


                        if (error instanceof NoConnectionError) {
                            Utils.getInstance().showToast(mContext, mContext.getString(R.string.no_network_connection));
                            listiners.onError(error);
                        } else if (error instanceof TimeoutError) {
                            Utils.getInstance().showToast(mContext, mContext.getString(R.string.time_out_error));
                            listiners.onError(error);
                        } else {
                            Utils.getInstance().showToast(mContext, mContext.getString(R.string.common_error_message));
                            listiners.onError(error);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    listiners.onError(error);
                }
            }

        }) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=utf-8");
//                    headers.put("Content-Type", "application/json");
                return headers;
            }

           /* @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }*/
        };


        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        if (ConnectionDetector.checkNetworkAvailablity(mContext)) {
//            VolleySingleton.getInstance(mContext).addToRequestQueue(request);
            Volley.newRequestQueue(mContext).add(request);
        } else {
            Utils.getInstance().showNoInternetAlertDialog(mContext, mContext.getResources().getString(R.string.alert_need_internet_connection), "Alert !");
            listiners.onError(null);
        }
    }


    public void addNewStringRequestPost(final Context mContext, final int requestType, String url, final HashMap<String, String> bundle, final NetworkListiners listiners) {
        this.mContext = mContext;
        this.requestType = requestType;
        this.url = url;
        this.listiners = listiners;
        this.bundle = bundle;

        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                AppBeanData data = null;
                Gson gson = new Gson();
                try {
                    if (listiners != null) {
                        if (requestType == RequestType.LOGOUT_REQUEST) {
//                            data = gson.fromJson(response.toString(), LoginSsoApi.class);
                        }
                        listiners.onCompleteResponse(requestType, data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    //      Utils.getInstance().setToastTypefaceforShort(mContext, mContext.getString(R.string.json_error));
                    listiners.onError(null);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (listiners != null) {
                    try {
                        Log.v("aman", "onErrorResponse called" + error);
                        if (error instanceof NoConnectionError) {
                            Utils.getInstance().showToast(mContext, mContext.getString(R.string.no_network_connection));
                            listiners.onError(error);
                        } else if (error instanceof TimeoutError) {
                            Utils.getInstance().showToast(mContext, mContext.getString(R.string.time_out_error));
                            listiners.onError(error);
                        }
                        if (error instanceof AuthFailureError) {
                            listiners.onError(error);
                        } else {
                            Utils.getInstance().showToast(mContext, mContext.getString(R.string.common_error_message));
                            listiners.onError(error);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }) {


            @Override
            protected Map<String, String> getParams() {
                Map<String, String> param = new HashMap<String, String>();
                param = bundle;
                return param;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded";
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        if (ConnectionDetector.checkNetworkAvailablity(mContext)) {
            VolleySingleton.getInstance(mContext).addToRequestQueue(request);
        } else {
            try {
                Utils.getInstance().showNoInternetAlertDialog(mContext, mContext.getResources().getString(R.string.alert_need_internet_connection), "Alert !");
                listiners.onError(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void cancelAllRequestQueue() {
        try {
            if (mContext != null && VolleySingleton.getInstance(mContext).getRequestQueue() != null) {
                VolleySingleton.getInstance(mContext).getRequestQueue().cancelAll("MULTIPART_REQUEST");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}