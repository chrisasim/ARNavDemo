package com.example.arnavdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.arnavdemo.mapping.DestinationFromId;
import com.example.arnavdemo.mapping.Location;
import com.example.arnavdemo.mapping.LocationFactory;

import java.util.ArrayList;

public class DestinationActivity extends AppCompatActivity {

    private ArrayList<Integer> locationPoint = new ArrayList<>();
    private ArrayList<Integer> destinationPolygon = new ArrayList<>();

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
    private Button submit;

    private EditText enterOfficeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_destination);

//        locationPoint = getIntent().getIntegerArrayListExtra("coords");
//        Log.d("coordinateX", String.valueOf(locationPoint.get(1)));

        enterOfficeId = findViewById(R.id.txtenterOfficeId);
        submit = findViewById(R.id.btnSubmit);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (enterOfficeId.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Enter the office id", Toast.LENGTH_SHORT).show();
                }
                else {
                    DestinationFromId destinationFromId = new DestinationFromId(enterOfficeId.getText().toString());
                    destinationPolygon = destinationFromId.getCoordinates();
                    if (destinationPolygon == null) {
                        Toast.makeText(getApplicationContext(), "No validate office id", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Intent intent = new Intent(DestinationActivity.this, ShowCoordinates.class);
                        intent.putIntegerArrayListExtra("polygonCoords", destinationPolygon);
                        locationPoint = getIntent().getIntegerArrayListExtra("coords");
                        intent.putIntegerArrayListExtra("pointCoords", locationPoint);
                        startActivity(intent);
                        Toast.makeText(getApplicationContext(), "You select your destination successfully", Toast.LENGTH_SHORT).show();
                    }

                }
                //hide keyboard
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);

            }
        });


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

    private void clickButton(Button destinationButton, String destination) {
        destinationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LocationFactory locationFactory = new LocationFactory();
                Location polygon = locationFactory.getLocation("DESTINATION", destination);
                destinationPolygon = polygon.getCoordinates();
                Intent intent = new Intent(DestinationActivity.this, ShowCoordinates.class);
                intent.putIntegerArrayListExtra("polygonCoords", destinationPolygon);
                locationPoint = getIntent().getIntegerArrayListExtra("coords");
                intent.putIntegerArrayListExtra("pointCoords", locationPoint);
                startActivity(intent);
                Toast.makeText(getApplicationContext(), "You select your destination successfully", Toast.LENGTH_SHORT).show();
            }
        });


    }
}