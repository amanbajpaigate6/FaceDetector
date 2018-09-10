package com.gate6.facedetectionlibrary.utilsPkg;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gate6.facedetectionlibrary.R;


public class Utils {

    private static Utils utils;

    public static Utils getInstance() {
        if (utils == null) {
            utils = new Utils();
        }

        return utils;
    }


    public boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public void showToast(Context mContext, String message) {
        Toast.makeText(mContext,message,Toast.LENGTH_SHORT).show();
    }

    public void showNoInternetAlertDialog(final Context con, String message, String title) {
        try {
            final Context context = con;
            final Dialog dialog = new Dialog(context);

            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.custom_logout_layout1);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.setCancelable(false);

            TextView titletv1 = (TextView) dialog.findViewById(R.id.header1);
            titletv1.setText(title);

            TextView titletv2 = (TextView) dialog.findViewById(R.id.alert_content);
            titletv2.setVisibility(View.VISIBLE);
            titletv2.setText(message);

            Button okButton = (Button) dialog.findViewById(R.id.btnDone);
            okButton.setText(context.getResources().getString(R.string.ok));
            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    dialog.dismiss();

                }
            });

            Button cancel_alert = (Button) dialog.findViewById(R.id.btnExit);
            cancel_alert.setVisibility(View.GONE);
            cancel_alert.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    dialog.dismiss();

                }
            });
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
