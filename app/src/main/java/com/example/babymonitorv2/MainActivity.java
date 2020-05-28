package com.example.babymonitorv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

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
                WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if(!wifiManager.isWifiEnabled()){
                    Toast.makeText(MainActivity.this, "Lütfen Wifi Bağlantınızı Kontrol Edin.",Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(MainActivity.this, BebekServisKayitActivity.class);
                ArrayList<String> permissionList = new ArrayList<>();
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                        permissionList.add(Manifest.permission.INTERNET);
                    }
                    else{
                        pass++;
                    }
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        permissionList.add(Manifest.permission.RECORD_AUDIO);
                    }
                    else{
                        pass+=2;
                    }
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
                    }
                    else{
                        pass+=4;
                    }
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                    else{
                        pass+=8;
                    }
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                    else{
                        pass+=16;
                    }

                    if(permissionList.size() != 0){
                        String array[] = new String[permissionList.size()];
                        permissionList.toArray(array);
                        ActivityCompat.requestPermissions(MainActivity.this, array, 402);
                    }
                }
                if(pass == 31 || Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                    startActivity(intent);
                pass = 0;
            }
        });

        ebeveynButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EbeveynServisKayitActivity.class);
                ArrayList<String> permissionList = new ArrayList<>();
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                        permissionList.add(Manifest.permission.INTERNET);
                    }
                    else{
                        pass++;
                    }
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        permissionList.add(Manifest.permission.RECORD_AUDIO);
                    }
                    else{
                        pass+=2;
                    }
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
                    }
                    else{
                        pass+=4;
                    }
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                    else{
                        pass+=8;
                    }
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                    else{
                        pass+=16;
                    }

                    if(permissionList.size() != 0){
                        String array[] = new String[permissionList.size()];
                        permissionList.toArray(array);
                        ActivityCompat.requestPermissions(MainActivity.this, array, 402);
                    }
                }
                if(pass == 31 || Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                    startActivity(intent);
                pass = 0;
            }
        });
    }
}
