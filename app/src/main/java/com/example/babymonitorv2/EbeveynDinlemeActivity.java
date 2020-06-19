package com.example.babymonitorv2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.ArrayList;

/**
 * @author Cihan
 */

public class EbeveynDinlemeActivity extends AppCompatActivity {
    private static final String TAG = "EbeveynDinlemeActivity";
    public static final String CHANNEL_ID = "EbeveynServiceChannel";

    public static boolean isServiceOpen = false;
    public static boolean newChild;
    private static Intent intent;
    private static String childName;

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ebeveyn_dinleme);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        if(!isServiceOpen) {
            if(newChild) {
                isServiceOpen = true;
                childName = getIntent().getStringExtra("ChildName");
                try {
                } catch (Exception e) {
                    e.printStackTrace();
                }

                createNotificationChannel();

                intent = new Intent(EbeveynDinlemeActivity.this, EbeveynDinlemeServis.class);
                intent.putExtra("HostAddress",getIntent().getStringExtra("HostAddress"));
                intent.putExtra("Port",getIntent().getIntExtra("Port",0));
                intent.putExtra("SoundUri",getIntent().getStringExtra("SoundUri"));
                intent.putExtra("ChildName", childName);
                startService(intent);
                newChild = false;
            }
        }
        else {
            if (newChild) {
                newChild = false;
                EbeveynDinlemeServis.addNewChild(getPackageName(), getIntent().getStringExtra("HostAddress"), getIntent().getIntExtra("Port", 0), getIntent().getStringExtra("SoundUri"), getIntent().getStringExtra("ChildName"));
            }
        }
    }


    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_ID,
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = (NotificationManager) getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.ebeveyn_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent in = new Intent(this, EbeveynServisKayitActivity.class);
        startActivity(in);
        return super.onOptionsItemSelected(item);
    }

    public static void initBooleanVariables(){
        isServiceOpen = false;
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if(!isChangingConfigurations())
            stopService(intent);
        super.onDestroy();
    }


    /*public static class EbeveynDinlemeServis extends Service {
        private static final String TAG = "EbeveynDinlemeServis";
        public static final String CHANNEL_ID = "EbeveynServiceChannel";
        private static Intent serviceIntent;
        private static Context serviceContext;
        private static ArrayList<CustomNotification> notificationList;


        public EbeveynDinlemeServis(){
            super();
        }

        @Override
        public void onCreate() {
            super.onCreate();
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            if(notificationList == null)
                notificationList = new ArrayList<>();

        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            serviceContext = this;
            newChild = true;
            serviceIntent = intent;
            addNewChild(getPackageName(),intent.getStringExtra("HostAddress"),intent.getIntExtra("Port",0),intent.getStringExtra("SoundUri"));

            return START_NOT_STICKY;
        }

        protected static void addNewChild(String packageName, String hostAddress, int port, String soundUri){
            if(newChild){
                newChild = false;
                notificationList.add(new CustomNotification(serviceContext,childName,packageName,hostAddress,port,CHANNEL_ID,soundUri));
            }
        }

        public static void checkChildArray(){
            for(CustomNotification notification : notificationList){
                if(notification.currentState != CustomNotification.State.FINISHED){
                    return;
                }
            }
            isServiceOpen = false;
            serviceContext.stopService(serviceIntent);
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onDestroy() {
            try {
                stopService(serviceIntent);

            } catch (Exception e) {
                e.printStackTrace();
            }

            super.onDestroy();
        }

        public static CustomNotification getNotificationList(int id){
            return notificationList.get(id);
        }
    }*/
}