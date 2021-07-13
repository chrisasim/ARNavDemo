package com.example.arnavdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class WelcomeActivity extends AppCompatActivity {
    
    public static final int BLUETOOTH_RED_CODE = 1;
    private String[] permission = {Manifest.permission.ACCESS_CHECKIN_PROPERTIES, Manifest.permission.ACCESS_FINE_LOCATION};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null)
        {
            Toast.makeText(getApplicationContext(), "Device does not support Bluetooth", Toast.LENGTH_LONG).show();
        }
        else
        {
            if (!bluetoothAdapter.isEnabled())
            {
                startActivityForResult(new Intent(bluetoothAdapter.ACTION_REQUEST_ENABLE), BLUETOOTH_RED_CODE);
                //Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_LONG).show();
            }

        }

        getUserPermission();

        Button btnContinue = findViewById(R.id.continue_button);

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, ExplanationApp.class);
                startActivity(intent);
            }
        });
    }
    private void getUserPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(permission, 0);
            }
        }
    }
    
}
