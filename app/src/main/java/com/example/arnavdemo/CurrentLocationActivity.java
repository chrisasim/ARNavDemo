package com.example.arnavdemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.arnavdemo.mapping.Location;
import com.example.arnavdemo.mapping.LocationFactory;

import java.util.ArrayList;

public class CurrentLocationActivity extends AppCompatActivity {

    private Button btnScanBarcode;
    private Button btnEntrance;
    private Button btnGraduateStudentOfficeA1;
    private Button btnBlekasOffice;
    private Button btnVlachosOffice;
    private Button btnLykasOffice;
    private Button btnGraduateStudentOfficeA5;
    private Button btnGraduateStudentOfficeA6;
    private Button btnZarrasOffice;
    private Button btnPolenakisOffice;
    private Button btnMamoulisOffice;
    private Button btnGraduateStudentOfficeA10;
    private Button btnSecretariat;
    private Button btnLaboratory;
    private Button btnTelecommunicationsLaboratory;
    private Button btnEdipMembers;
    private ArrayList<Integer> coordinates;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_location);

        btnScanBarcode =findViewById(R.id.qrCodeButton);
        btnEntrance = findViewById((R.id.btnEntrance));
        btnGraduateStudentOfficeA1 = findViewById(R.id.btnGraduateStudentOfficeA1);
        btnBlekasOffice = findViewById(R.id.btnBlekasOffice);
        btnVlachosOffice = findViewById(R.id.btnVlachosOffice);
        btnLykasOffice = findViewById(R.id.btnLykasOffice);
        btnGraduateStudentOfficeA5 =findViewById(R.id.btnGraduateStudentOfficeA5);
        btnGraduateStudentOfficeA6 = findViewById((R.id.btnGraduateStudentOfficeA6));
        btnZarrasOffice = findViewById(R.id.btnZarrasOffice);
        btnPolenakisOffice = findViewById(R.id.btnPolenakisOffice);
        btnMamoulisOffice = findViewById(R.id.btnMamoulisOffice);
        btnGraduateStudentOfficeA10 = findViewById(R.id.btnGraduateStudentOfficeA10);
        btnSecretariat = findViewById(R.id.btnSecretariat);
        btnLaboratory = findViewById(R.id.btnLaboratory);
        btnTelecommunicationsLaboratory = findViewById(R.id.btnTelecommunicationsLaboratory);
        btnEdipMembers = findViewById(R.id.btnEdipMembers);

        btnScanBarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator integrator = new IntentIntegrator(CurrentLocationActivity.this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
                integrator.setPrompt("Scan a barcode or QR code");
                integrator.setBeepEnabled(true);
                integrator.setBarcodeImageEnabled(true);
                integrator.initiateScan();
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
                Intent intent = new Intent(CurrentLocationActivity.this, DestinationActivity.class);
                intent.putIntegerArrayListExtra("coords", coordinates);
                startActivity(intent);
                Toast.makeText(getApplicationContext(), "You set your location successfully", Toast.LENGTH_SHORT).show();
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
                //
                Intent intent = new Intent(CurrentLocationActivity.this, DestinationActivity.class);
                intent.putIntegerArrayListExtra("coords", coordinates);
                startActivity(intent);
                Toast.makeText(getApplicationContext(), "You set your location successfully", Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), String.valueOf(result.getFormatName()), Toast.LENGTH_SHORT);
                Toast.makeText(getApplicationContext(), String.valueOf(result.getContents()), Toast.LENGTH_SHORT);
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
