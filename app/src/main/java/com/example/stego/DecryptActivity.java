package com.example.stego;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class DecryptActivity extends AppCompatActivity {

    private Button oneway;
    private Button twoway;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decrypt);

        getSupportActionBar().setTitle("Decrypt");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        oneway= findViewById(R.id.decryptOneWay);
        twoway= findViewById(R.id.decryptTwoway);

        oneway.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1=new Intent(DecryptActivity.this,DecryptOneWayActivity.class);
                startActivity(intent1);
            }
        });

        twoway.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent2= new Intent(DecryptActivity.this,DecryptTwoWayActivity.class);
                startActivity(intent2);
            }
        });
    }
}
