package com.example.babymonitorv2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * @author Cihan
 */

public class BebekServisKayitActivity extends AppCompatActivity {
    private static final String TAG = "BebekServisKayitActivit";
    private static String serviceName = "YtuceBabyMonitor";
    private static ServerSocket serverSocket;
    private static Socket mediaSocket;
    private static boolean isRegistrationEnabled = false;
    private static boolean giveNewPin = false;
    private static final int REQUEST_CODE = 800;
    private static NsdManager.DiscoveryListener discoveryListener;
    private static NsdManager.RegistrationListener registrationListener;
    private static NsdManager nsdManager;
    private static boolean showError = false;

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static void showErrorOnReturn(){
        showError = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bebek_servis_kayit);
        final TextView sifreTv = (TextView) findViewById(R.id.bebekKayitSifreTextView);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        if(nsdManager == null){
            nsdManager = (NsdManager)getSystemService(Context.NSD_SERVICE);
        }
        
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if(!isRegistrationEnabled) {
            try {
                Log.d(TAG, "onCreate: girdi");
                final ArrayList<Integer> pinList = checkPinCode();
                Thread.sleep(500);
                nsdManager.stopServiceDiscovery(discoveryListener);
                final int pinCode = createPinCode(pinList);
                sifreTv.setText(Integer.toString(pinCode));
                registerService(pinCode);
                Thread registerThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mediaSocket = serverSocket.accept();
                            Log.d(TAG, "run: girdi");
                            nsdManager.unregisterService(registrationListener);
                            isRegistrationEnabled = false;
                            Intent intent = new Intent(BebekServisKayitActivity.this, BebekMonitorActivity.class);
                            Log.d(TAG, "run: Intent Change");
                            startActivityForResult(intent, REQUEST_CODE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            registerThread.start();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE){
            if(showError) {
                AlertDialog alertDialog = new AlertDialog.Builder(this)
                        .setTitle("Hata")
                        .setMessage("Karşı cihaz ile olan bağlantınız koptu. Lütfen Tekrar deneyin")
                        .setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).create();
                alertDialog.show();
                showError = false;
            }
            giveNewPin = true;
            recreate();
        }
    }

    public static Socket getBabySocket(){
        return mediaSocket;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(!giveNewPin) {
            ((TextView) findViewById(R.id.bebekKayitSifreTextView)).setText(savedInstanceState.getString("Pin"));
        }
        else{
            giveNewPin = false;
        }
    }

    
    
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if(!giveNewPin) {
            outState.putString("Pin", ((TextView) findViewById(R.id.bebekKayitSifreTextView)).getText().toString());
        }
        super.onSaveInstanceState(outState);
    }

    private ArrayList<Integer> checkPinCode() {
        final ArrayList<Integer> pinList = new ArrayList<>();

        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.d(TAG, "onStartDiscoveryFailed: Discovery Failed");
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.d(TAG, "onStopDiscoveryFailed: Can't Stop Discovery");
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d(TAG, "onDiscoveryStarted: Discovery Started");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d(TAG, "onDiscoveryStopped: Discovery Stopped");
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "onServiceFound: Service Found");

                if (serviceInfo.getServiceName().contains("YtuceBabyMonitor")) {
                    int pin = Integer.parseInt(serviceInfo.getServiceName().substring(16,22));
                    Log.d(TAG, "onServiceFound: pinArama = " + pin);
                    pinList.add(pin);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "onServiceLost: Service Lost");
            }
        };

        nsdManager.discoverServices("_ytucebabymonitor._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener);

        return pinList;
    }

    private int createPinCode(ArrayList<Integer> pinList){

        int newPin = (int)(Math.random() * 999999.0);
        if(newPin < 100000)
            newPin += 100000;

        for(int i = 0; i < pinList.size(); i++){
            if(newPin == pinList.get(i)){
                newPin = (int)(Math.random() * 999999.0);
                if(newPin < 100000)
                    newPin += 100000;
                i = 0;
            }
        }

        return newPin;
    }

    private void registerService(final int pinCode) throws IOException {
        Log.d(TAG, "registerService: girdi");
            serverSocket = new ServerSocket(0);
            int port = serverSocket.getLocalPort();

            NsdServiceInfo nsdServiceInfo = new NsdServiceInfo();
            nsdServiceInfo.setServiceName("YtuceBabyMonitor"+pinCode);
            nsdServiceInfo.setServiceType("_ytucebabymonitor._tcp");
            nsdServiceInfo.setPort(port);

            Log.d(TAG, "registerService: port = " + port);

            Log.d(TAG, "registerService: Pin Code = " + Integer.toString(pinCode));

            registrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "onRegistrationFailed: Registration Failed");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "onUnregistrationFailed: Unregistration Failed");
            }

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "onServiceRegistered: Service Registered ");
                serviceName = serviceInfo.getServiceName();
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "onServiceUnregistered: Service Unregistered");

            }
        };
            nsdManager.registerService(nsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
            isRegistrationEnabled = true;
    }

    @Override
    public void onDestroy() {
        if(!isChangingConfigurations()) {
            Log.d(TAG, "onDestroy: girdi");
            if (isRegistrationEnabled) {
                nsdManager.unregisterService(registrationListener);
                isRegistrationEnabled = false;
            }
        }
        super.onDestroy();
    }

}
