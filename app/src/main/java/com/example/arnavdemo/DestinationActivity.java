package com.example.arnavdemo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.arnavdemo.mapping.DestinationFromId;
import com.example.arnavdemo.mapping.Location;
import com.example.arnavdemo.mapping.LocationFactory;

import java.util.ArrayList;

public class DestinationActivity extends AppCompatActivity {

    private ArrayList<Integer> locationPoint = new ArrayList<>();
    private String url;
    private ArrayList<Integer> destinationPolygon = new ArrayList<>();

    private EditText enterOfficeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_destination);

//        locationPoint = getIntent().getIntegerArrayListExtra("coords");
//        Log.d("coordinateX", String.valueOf(locationPoint.get(1)));

        url = getIntent().getStringExtra("url");
        enterOfficeId = findViewById(R.id.txtenterOfficeId);
        Button submit = findViewById(R.id.btnSubmit);

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
                        Intent intent = new Intent(DestinationActivity.this, ARNavigation.class);
                        intent.putIntegerArrayListExtra("polygonCoords", destinationPolygon);
                        locationPoint = getIntent().getIntegerArrayListExtra("coords");
                        intent.putIntegerArrayListExtra("pointCoords", locationPoint);
                        intent.putExtra("urlCoords", url);
                        startActivity(intent);
                        Toast.makeText(getApplicationContext(), "You select your destination successfully", Toast.LENGTH_SHORT).show();
                    }

                }
                //hide keyboard
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);

            }
        });


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
                Intent intent = new Intent(DestinationActivity.this, ARNavigation.class);
                intent.putIntegerArrayListExtra("polygonCoords", destinationPolygon);
                locationPoint = getIntent().getIntegerArrayListExtra("coords");
                intent.putIntegerArrayListExtra("pointCoords", locationPoint);
                intent.putExtra("urlCoords", url);
                startActivity(intent);
                Toast.makeText(getApplicationContext(), "You select your destination successfully", Toast.LENGTH_SHORT).show();
            }
        });


    }
}