package com.example.stego;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class DecryptTwoWayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decrypt_two_way);

        getSupportActionBar().setTitle("Two Way Decryption");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
