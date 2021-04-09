package com.example.arnavdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class ShowCoordinates extends AppCompatActivity {

    private TextView polygon;
    private TextView point;
    private TextView url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_coordinates);

        polygon = findViewById(R.id.txtPolygonCoords);
        point = findViewById(R.id.txtPointCoords);
        url = findViewById(R.id.txtUrl);


        polygon.setText("Your destination keeps in polygon with coords: " + String.valueOf(getIntent().getIntegerArrayListExtra("polygonCoords")));
        point.setText("Your current location is found: " + String.valueOf(getIntent().getIntegerArrayListExtra("pointCoords")));
        url.setText("Your current location from qr code is found: " + getIntent().getStringExtra("urlCoords"));

    }
}