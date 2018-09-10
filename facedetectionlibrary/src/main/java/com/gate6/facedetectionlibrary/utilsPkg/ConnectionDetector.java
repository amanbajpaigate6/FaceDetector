package com.gate6.facedetectionlibrary.utilsPkg;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


public class ConnectionDetector {

    private Context _context;
    private static ConnectionDetector connectionDetector = null;

    public static ConnectionDetector getinstance() {
        if (connectionDetector == null) {
            connectionDetector = new ConnectionDetector();
        }
        return connectionDetector;
    }

    public static boolean isConnectingToInternet(Context mContext) {
        ConnectivityManager connectivity = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }

        }
        return false;
    }

    public static boolean checkNetworkAvailablity(Context mContext) {
        try {

            ConnectivityManager cMgr = (ConnectivityManager) mContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo netInfo = cMgr.getActiveNetworkInfo();

            return netInfo != null;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
//            throw new NetConnectException(
//                    NetConnectException.NO_NETWORK_EXCEPTION,
//                    NetConnectException.TYPE_CONNECTION,
//                    NetConnectException.NO_NETWORK_CONNECTION);

        }
    }

}
