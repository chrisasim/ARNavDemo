package com.example.indoornavigation;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.indoornavigation.mapping.DestinationFromId;

import java.util.ArrayList;


public class DestinationActivity extends AppCompatActivity implements View.OnClickListener {

//    public static final String GRADUATE_STUDENT_OFFICE_A1 = "graduateStudentOfficeA1";
    public static final String BLEKAS_OFFICE = "blekasOffice";
    public static final String VLACHOS_OFFICE = "vlachosOffice";
    public static final String LYKAS_OFFICE = "lykasOffice";
//    public static final String GRADUATE_STUDENT_OFFICE_A5 = "graduateStudentOfficeA5";
//    public static final String GRADUATE_STUDENT_OFFICE_A6 = "graduateStudentOfficeA6";
    public static final String ZARRAS_OFFICE = "zarrasOffice";
    public static final String POLENAKIS_OFFICE = "polenakisOffice";
    public static final String MAMOULIS_OFFICE = "mamoulisOffice";
//    public static final String GRADUATE_STUDENT_OFFICE_A10 = "graduateStudentOfficeA10";
//    public static final String SECRETARIAT = "secretariat";
//    public static final String LABORATORY = "laboratory";
//    public static final String TELECOMMUNICATIONS_LABORATORY = "telecommunicationsLaboratory";
//    public static final String EDIP_MEMBERS = "edipMembers";
    public static final String FROM = "from";
    public static final String COORDS_OF_CURRENT_POS = "coordsOfCurrentPos";
    public static final String COORDS_OF_DESTINATION_ID = "coordsOfDestinationId";
    public static final String COORDS_OF_ENTRANCE = "coordsOfEntrance";
    public static String SELECT_DESTINATION_FROM = null;

    private ArrayList<Integer> locationPoint;
    private ArrayList<Integer> coordsOfEntrance;

    private EditText enterOfficeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_destination);


        coordsOfEntrance =  getIntent().getIntegerArrayListExtra(CurrentLocationActivity.COORDS_OF_ENTRANCE);
        locationPoint =  getIntent().getIntegerArrayListExtra(CurrentLocationActivity.COORDS_OF_LOCATION);
        enterOfficeId = findViewById(R.id.txtenterOfficeId);
        Button submit = findViewById(R.id.btnSubmit);

        submit.setOnClickListener(v -> {
            if (enterOfficeId.getText().toString().isEmpty()) {
                Toast.makeText(getApplicationContext(), "Enter the office id", Toast.LENGTH_SHORT).show();
            }
            else {
                DestinationFromId destinationFromId = new DestinationFromId(enterOfficeId.getText().toString());
                ArrayList<Integer> destinationPolygon = destinationFromId.getCoordinates();
                if (destinationPolygon == null) {
                    Toast.makeText(getApplicationContext(), "No validate office id", Toast.LENGTH_SHORT).show();
                }
                else {
                    Intent intent = new Intent(DestinationActivity.this, ARNavigation.class);
                    intent.putIntegerArrayListExtra(COORDS_OF_DESTINATION_ID, destinationPolygon);
                    intent.putIntegerArrayListExtra(COORDS_OF_CURRENT_POS, locationPoint);
                    intent.putExtra(COORDS_OF_ENTRANCE, coordsOfEntrance);
                    SELECT_DESTINATION_FROM = "fromId";
                    startActivity(intent);
                    Toast.makeText(getApplicationContext(), "You select your destination successfully", Toast.LENGTH_SHORT).show();
                }

            }
            //hide keyboard
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);

        });

//        Button btnGraduateStudentOfficeA1 = findViewById(R.id.btnGraduateStudentOfficeA1);
        Button btnBlekasOffice = findViewById(R.id.btnBlekasOffice);
        Button btnVlachosOffice = findViewById(R.id.btnVlachosOffice);
        Button btnLykasOffice = findViewById(R.id.btnLykasOffice);
//        Button btnGraduateStudentOfficeA5 = findViewById(R.id.btnGraduateStudentOfficeA5);
//        Button btnGraduateStudentOfficeA6 = findViewById((R.id.btnGraduateStudentOfficeA6));
        Button btnZarrasOffice = findViewById(R.id.btnZarrasOffice);
        Button btnPolenakisOffice = findViewById(R.id.btnPolenakisOffice);
        Button btnMamoulisOffice = findViewById(R.id.btnMamoulisOffice);
//        Button btnGraduateStudentOfficeA10 = findViewById(R.id.btnGraduateStudentOfficeA10);
//        Button btnSecretariat = findViewById(R.id.btnSecretariat);
//        Button btnLaboratory = findViewById(R.id.btnLaboratory);
//        Button btnTelecommunicationsLaboratory = findViewById(R.id.btnTelecommunicationsLaboratory);
//        Button btnEdipMembers = findViewById(R.id.btnEdipMembers);
//        btnGraduateStudentOfficeA1.setOnClickListener(this);
        btnBlekasOffice.setOnClickListener(this);
        btnVlachosOffice.setOnClickListener(this);
        btnLykasOffice.setOnClickListener(this);
//        btnGraduateStudentOfficeA5.setOnClickListener(this);
//        btnGraduateStudentOfficeA6.setOnClickListener(this);
        btnZarrasOffice.setOnClickListener(this);
        btnPolenakisOffice.setOnClickListener(this);
        btnMamoulisOffice.setOnClickListener(this);
//        btnGraduateStudentOfficeA10.setOnClickListener(this);
//        btnSecretariat.setOnClickListener(this);
//        btnLaboratory.setOnClickListener(this);
//        btnTelecommunicationsLaboratory.setOnClickListener(this);
//        btnEdipMembers.setOnClickListener(this);

    }





    @Override
    public void onClick(View v) {
        switch (v.getId()) {
//            case R.id.btnGraduateStudentOfficeA1:
//                goToCameraActivity(GRADUATE_STUDENT_OFFICE_A1);
//                break;
            case R.id.btnBlekasOffice:
                goToCameraActivity(BLEKAS_OFFICE);
                break;
            case R.id.btnVlachosOffice:
                goToCameraActivity(VLACHOS_OFFICE);
                break;
            case R.id.btnLykasOffice:
                goToCameraActivity(LYKAS_OFFICE);
                break;
//            case R.id.btnGraduateStudentOfficeA5:
//                goToCameraActivity(GRADUATE_STUDENT_OFFICE_A5);
//                break;
//            case R.id.btnGraduateStudentOfficeA6:
//                goToCameraActivity(GRADUATE_STUDENT_OFFICE_A6);
//                break;
            case R.id.btnZarrasOffice:
                goToCameraActivity(ZARRAS_OFFICE);
                break;
            case R.id.btnPolenakisOffice:
                goToCameraActivity(POLENAKIS_OFFICE);
                break;
            case R.id.btnMamoulisOffice:
                goToCameraActivity(MAMOULIS_OFFICE);
                break;
//            case R.id.btnGraduateStudentOfficeA10:
//                goToCameraActivity(GRADUATE_STUDENT_OFFICE_A10);
//                break;
//            case R.id.btnSecretariat:
//                goToCameraActivity(SECRETARIAT);
//                break;
//            case R.id.btnLaboratory:
//                goToCameraActivity(LABORATORY);
//                break;
//            case R.id.btnTelecommunicationsLaboratory:
//                goToCameraActivity(TELECOMMUNICATIONS_LABORATORY);
//                break;
//            case R.id.btnEdipMembers:
//                goToCameraActivity(EDIP_MEMBERS);
//                break;
        }
    }


    private void goToCameraActivity(String Section) {
        Intent intent = new Intent(DestinationActivity.this, ARNavigation.class);
        intent.putExtra(FROM, Section);
        intent.putIntegerArrayListExtra(COORDS_OF_CURRENT_POS, locationPoint);
        intent.putExtra(COORDS_OF_ENTRANCE, coordsOfEntrance);
        SELECT_DESTINATION_FROM = "fromMenu";
        startActivity(intent);
        Toast.makeText(getApplicationContext(), "You select your destination successfully", Toast.LENGTH_SHORT).show();
    }
}