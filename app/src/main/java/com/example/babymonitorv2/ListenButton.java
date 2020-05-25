package com.example.babymonitorv2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ListenButton extends BroadcastReceiver {
    private static final String TAG = "ListenButton";
    public ListenButton(){
        super();
    }
    @Override
    public void onReceive(Context context, Intent in) {
        int id = in.getIntExtra("Id",0);
        Log.d(TAG, "onReceive: Id = " + id);
        EbeveynDinlemeActivity.EbeveynDinlemeServis.getNotificationList(id).onReceiveListen(context, in);
    }
}