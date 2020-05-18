package com.example.babymonitorv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * @author Cihan
 */

public class MainActivity extends AppCompatActivity {
    int pass = 0;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,"Lütfen Gerekli İzinleri Ayarlayın", Toast.LENGTH_LONG).show();
            }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button bebekButton = findViewById(R.id.modBebekButton);
        Button ebeveynButton = findViewById(R.id.modEbeveynButton);


        bebekButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BebekServisKayitActivity.class);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.INTERNET}, 400);
                    }
                    else{
                        pass++;
                    }
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 401);
                    }
                    else{
                        pass+=2;
                    }
                }
                if(pass == 3 || Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                    startActivity(intent);
                pass = 0;
            }
        });

        ebeveynButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EbeveynServisKayitActivity.class);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.INTERNET}, 400);
                    }
                    else{
                        pass++;
                    }
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 401);
                    }
                    else{
                        pass+=2;
                    }
                }
                if(pass == 3 || Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                    startActivity(intent);
                pass = 0;
            }
        });
    }
}
