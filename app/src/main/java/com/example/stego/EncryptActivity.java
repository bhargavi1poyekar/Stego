package com.example.stego;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class EncryptActivity extends AppCompatActivity {

    private Button oneway;
    private Button twoway;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encrypt);

        getSupportActionBar().setTitle("Encrypt");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        oneway= findViewById(R.id.encryptOneWay);
        twoway= findViewById(R.id.encryptTwoWay);

        oneway.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1=new Intent(EncryptActivity.this,EncryptOneWayActivity.class);
                startActivity(intent1);
            }
        });

        twoway.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent2= new Intent(EncryptActivity.this,ImageEncryptActivity.class);
                startActivity(intent2);
            }
        });
    }
}
