package com.example.babymonitorv2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.StrictMode;

import java.util.ArrayList;

import androidx.annotation.Nullable;

public class EbeveynDinlemeServis extends Service {
    private final String TAG = "EbeveynDinlemeServis";
    public final String CHANNEL_ID = "EbeveynServiceChannel";
    private Intent serviceIntent;
    private Context serviceContext;
    private ArrayList<CustomNotification> notificationList;
    private static EbeveynDinlemeServis instance;


    public EbeveynDinlemeServis(){
        super();
    }

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        instance = this;
        if(intent == null)
            return START_STICKY;
        if(notificationList == null)
            notificationList = new ArrayList<>();


        serviceContext = this;
        serviceIntent = intent;
        addNewChild(getPackageName(),intent.getStringExtra("HostAddress"),intent.getIntExtra("Port",0),intent.getStringExtra("SoundUri"), intent.getStringExtra("ChildName"));
        return START_STICKY;
    }

    protected static void addNewChild(String packageName, String hostAddress, int port, String soundUri, String childName){
        instance.notificationList.add(new CustomNotification(instance.serviceContext,childName,packageName,hostAddress,port,instance.CHANNEL_ID,soundUri));
    }

    public static void checkChildArray(){
        for(CustomNotification notification : instance.notificationList){
            if(notification.currentState != CustomNotification.State.FINISHED){
                return;
            }
        }
        EbeveynDinlemeActivity.isServiceOpen = false;
        instance.serviceContext.stopService(instance.serviceIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    @Override
    public void onDestroy() {
        try {
            CustomNotification.setAtomicIntToZero();
            for(CustomNotification notification : notificationList){
                if(notification.currentState != CustomNotification.State.FINISHED){
                    notification.onReceiveClose(this, serviceIntent, true);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

    public static CustomNotification getNotificationList(int id){
        return instance.notificationList.get(id);
    }
}