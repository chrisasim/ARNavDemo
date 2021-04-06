package com.example.arnavdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ExplanationApp extends AppCompatActivity {
    private Button startDemo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explanation_app);

        startDemo = findViewById(R.id.startDemoButton);

        startDemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ExplanationApp.this, CurrentLocation.class);
                startActivity(intent);
            }
        });
    }
}