package com.example.babymonitorv2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CloseButton extends BroadcastReceiver {
    private static final String TAG = "CloseButton";
    public CloseButton(){
        super();
    }
    @Override
    public void onReceive(Context context, Intent in) {
        int id = in.getIntExtra("Id",0);
        Log.d(TAG, "onReceive: Id = " + id);
        EbeveynDinlemeServis.getNotificationList(id).onReceiveClose(context, in ,false);
    }
}