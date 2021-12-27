package com.example.spotter;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Settings extends AppCompatActivity {
    MyLocationService mService;
    Button disableService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        disableService = findViewById(R.id.disableService);

        findViewById(R.id.buttonBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        disableService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainHome.removeService();
                disableService.setClickable(false);
            }
        });
    }
}
