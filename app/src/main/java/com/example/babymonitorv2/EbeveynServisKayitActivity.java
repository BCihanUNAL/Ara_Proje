package com.example.babymonitorv2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.net.Socket;
import java.util.HashMap;

/**
 * @author Cihan
 */

public class EbeveynServisKayitActivity extends AppCompatActivity {
    private static HashMap<Integer, NsdServiceInfo> serviceInfoHashMap;
    private static final String serviceType = "_ytucebabymonitor._tcp.";
    private static final String TAG = "EbeveynServisKayitActiv";
    //private static Socket mediaSocket;
    private static NsdManager.ResolveListener resolveListener;
    private static NsdManager.DiscoveryListener discoveryListener;
    private static boolean isDiscoveryEnabled = false;
    private static NsdManager nsdManager;
    private final int REQUEST_CODE = 801;

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ebeveyn_servis_kayit);
        Log.d(TAG, "onCreate: yeni bastan" + isDiscoveryEnabled);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);



        if(nsdManager == null){
            nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        }

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if(!isDiscoveryEnabled) {
            serviceInfoHashMap = new HashMap<>();
            final EditText ebeveynPinCode = findViewById(R.id.ebeveynKayitPinEditText);
            Button ebeveynBaglanti = findViewById(R.id.ebeveynKayitServisBaglantiButton);
            discoverService();
            Log.d(TAG, "onCreate: basliyor");
            ebeveynBaglanti.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if(!wifiManager.isWifiEnabled()){
                        Toast.makeText(EbeveynServisKayitActivity.this, "Lütfen Wifi Bağlantınızı Kontrol Edin.",Toast.LENGTH_LONG).show();
                        return;
                    }
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (serviceInfoHashMap.containsKey(Integer.parseInt("0" + ebeveynPinCode.getText().toString()))) {
                                try {
                                    NsdServiceInfo nsdServiceInfo = serviceInfoHashMap.get(Integer.parseInt(ebeveynPinCode.getText().toString()));
                                    String serviceName = nsdServiceInfo.getServiceName();
                                    serviceName = serviceName.replace("\\\\032", " ");
                                    serviceName = serviceName.replace("\\032", " ");
                                    //mediaSocket = new Socket(nsdServiceInfo.getHost().getHostAddress(), nsdServiceInfo.getPort());
                                    EbeveynDinlemeActivity.initBooleanVariables();
                                    nsdManager.stopServiceDiscovery(discoveryListener);
                                    isDiscoveryEnabled = false;
                                    Intent intent = new Intent(EbeveynServisKayitActivity.this, EbeveynDinlemeActivity.class);
                                    intent.putExtra("ServiceName", serviceName);
                                    intent.putExtra("ChildName",((EditText)findViewById(R.id.ebeveynKayitIsimEditText)).getText().toString());
                                    intent.putExtra("HostAddress",nsdServiceInfo.getHost().getHostAddress());
                                    intent.putExtra("Port",nsdServiceInfo.getPort());
                                    startActivityForResult(intent, REQUEST_CODE);

                                   // Log.d(TAG, "onClick: Socket connected " + mediaSocket.getPort() + " " + mediaSocket.getInetAddress().toString());

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    thread.start();
                }
            });
        }
    }

   // public static Socket getParentSocket(){
   //     return mediaSocket;
   // }

    private void discoverService(){
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.d(TAG, "onStartDiscoveryFailed: Can't Start Discovery");
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
                if(!serviceInfo.getServiceType().equals(serviceType)){
                    Log.d(TAG, "onServiceFound: Wrong Service Found. Service Type = " + serviceInfo.getServiceType());
                }
                else{
                    if(serviceInfo.getServiceName().contains("YtuceBabyMonitor")){
                        // Resolvelistener'i sadece bir kere tanimla
                        resolveListener = new NsdManager.ResolveListener() {
                            @Override
                            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                                Log.d(TAG, "onResolveFailed: Resolve Failed");
                            }

                            @Override
                            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                                Log.d(TAG, "onServiceResolved: Service Resolved");

                                int pinCode = Integer.parseInt(serviceInfo.getServiceName().substring(16,22));

                                Log.d(TAG, "onServiceResolved: PinCode = " + pinCode);

                                serviceInfoHashMap.put(pinCode,serviceInfo);
                            }
                        };
                        nsdManager.resolveService(serviceInfo,resolveListener);
                    }
                }

            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "onServiceLost: Service Lost");
            }
        };
        nsdManager.discoverServices(serviceType,NsdManager.PROTOCOL_DNS_SD,discoveryListener);
        isDiscoveryEnabled = true;
    }


    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        if (isDiscoveryEnabled) {
            nsdManager.stopServiceDiscovery(discoveryListener);
            isDiscoveryEnabled = false;
            Log.d(TAG, "onDestroy: " + isDiscoveryEnabled);
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE){
            /*Intent intent = getIntent();
            finish();
            startActivity(intent);*/
            recreate();
            Log.d(TAG, "onActivityResult: intent" + isDiscoveryEnabled);
        }
    }
}
