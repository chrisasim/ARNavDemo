package com.example.arnavdemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.arnavdemo.mapping.Location;
import com.example.arnavdemo.mapping.LocationFactory;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;

public class CurrentLocationActivity extends AppCompatActivity {

    private ArrayList<Integer> coordinates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_location);

        Button btnScanBarcode = findViewById(R.id.qrCodeButton);
        Button btnEntrance = findViewById((R.id.btnEntrance));
        Button btnGraduateStudentOfficeA1 = findViewById(R.id.btnGraduateStudentOfficeA1);
        Button btnBlekasOffice = findViewById(R.id.btnBlekasOffice);
        Button btnVlachosOffice = findViewById(R.id.btnVlachosOffice);
        Button btnLykasOffice = findViewById(R.id.btnLykasOffice);
        Button btnGraduateStudentOfficeA5 = findViewById(R.id.btnGraduateStudentOfficeA5);
        Button btnGraduateStudentOfficeA6 = findViewById((R.id.btnGraduateStudentOfficeA6));
        Button btnZarrasOffice = findViewById(R.id.btnZarrasOffice);
        Button btnPolenakisOffice = findViewById(R.id.btnPolenakisOffice);
        Button btnMamoulisOffice = findViewById(R.id.btnMamoulisOffice);
        Button btnGraduateStudentOfficeA10 = findViewById(R.id.btnGraduateStudentOfficeA10);
        Button btnSecretariat = findViewById(R.id.btnSecretariat);
        Button btnLaboratory = findViewById(R.id.btnLaboratory);
        Button btnTelecommunicationsLaboratory = findViewById(R.id.btnTelecommunicationsLaboratory);
        Button btnEdipMembers = findViewById(R.id.btnEdipMembers);

        btnScanBarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator integrator = new IntentIntegrator(CurrentLocationActivity.this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
                integrator.setPrompt("Scan a barcode or QR code");
                integrator.setBeepEnabled(true);
                integrator.setBarcodeImageEnabled(true);
                integrator.initiateScan();
                //startActivity(new Intent(CurrentLocationActivity.this, ScannedBarcodeActivity.class));
            }
        });

        clickButton(btnEntrance, "entrance");

        clickButton(btnGraduateStudentOfficeA1, "graduateStudentOfficeA1");

        clickButton(btnBlekasOffice, "blekasOffice");

        clickButton(btnVlachosOffice, "vlachosOffice");

        clickButton(btnLykasOffice, "lykasOffice");

        clickButton(btnGraduateStudentOfficeA5, "graduateStudentOfficeA5");

        clickButton(btnGraduateStudentOfficeA6, "graduateStudentOfficeA6");

        clickButton(btnZarrasOffice, "zarrasOffice");

        clickButton(btnPolenakisOffice, "polenakisOffice");

        clickButton(btnMamoulisOffice, "mamoulisOffice");

        clickButton(btnGraduateStudentOfficeA10, "graduateStudentOfficeA10");

        clickButton(btnSecretariat, "secretariat");

        clickButton(btnLaboratory, "laboratory");

        clickButton(btnTelecommunicationsLaboratory, "telecommunicationsLaboratory");

        clickButton(btnEdipMembers, "edipMembers");
    }

    private void clickButton(Button locationButton, String location) {
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LocationFactory locationFactory = new LocationFactory();
                Location point = locationFactory.getLocation("CURRENTLOCATION", location);
                coordinates = point.getCoordinates();
//                Intent intent = new Intent(CurrentLocationActivity.this, DestinationActivity.class);
//                intent.putIntegerArrayListExtra("coords", coordinates);
//                startActivity(intent);
//                Toast.makeText(getApplicationContext(), "You set your location successfully", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result  = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result!=null)
        {
            if(result.getContents()==null)
            {
                Toast.makeText(getBaseContext(), "Cancelled", Toast.LENGTH_LONG).show();
            }
            else
            {

                Intent intent = new Intent(CurrentLocationActivity.this, DestinationActivity.class);
                intent.putIntegerArrayListExtra("coords", coordinates);
                startActivity(intent);
                Toast.makeText(getApplicationContext(), "You set your location successfully", Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), String.valueOf(result.getFormatName()), Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), String.valueOf(result.getContents()), Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


}