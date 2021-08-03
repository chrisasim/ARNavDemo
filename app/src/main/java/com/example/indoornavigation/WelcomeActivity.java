package com.example.indoornavigation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {
    private String[] permission = {Manifest.permission.ACCESS_CHECKIN_PROPERTIES, Manifest.permission.ACCESS_FINE_LOCATION};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        getUserPermission();

        Button btnContinue = findViewById(R.id.continue_button);

        btnContinue.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, ExplanationApp.class);
            startActivity(intent);
        });
    }


    private void getUserPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permission, 0);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
        {
            Toast.makeText(WelcomeActivity.this, "Bluetooth is ON", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(WelcomeActivity.this, "Bluetooth operation is cancelled", Toast.LENGTH_SHORT).show();
        }
    }
}