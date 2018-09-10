package com.gate6.facedemo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.gate6.facedemo.R;


public class HomeActivity extends AppCompatActivity {

    private TextView home_text;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actiivty_home);
        home_text = (TextView) findViewById(R.id.home_text);
        if (getIntent() != null) {
            String name = getIntent().getStringExtra("USER_NAME");
            home_text.setText("Welcome " + name);
        }
        findViewById(R.id.logout_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent(HomeActivity.this, LogoutActivity.class);
//                startActivity(intent);

                Intent intent = new Intent(HomeActivity.this, SplashActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
