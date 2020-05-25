package com.example.babymonitorv2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static android.content.Context.VIBRATOR_SERVICE;

public class CustomNotification {
    private Notification notification = null;
    private int NOTIFICATION_ID;
    private boolean isVoiceButtonSet;
    private boolean trigger;
    private String CHANNEL_ID;
    private String childName;
    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private Timer timer;
    public enum State{RUNNING, FINISHED};
    public State currentState;
    private Context context;
    private boolean isCrying;
    private RemoteViews remoteViews;
    private static final String TAG = "CustomNotification";
    private final static AtomicInteger atomicInt = new AtomicInteger(0);
    private final static  AtomicInteger atomicErrorInt = new AtomicInteger(50000);

    public CustomNotification(final Context context, String childName, String packageName, String hostName, int port, String CHANNEL_ID){
        try {
            this.socket = new Socket(hostName, port);
            this.context = context;
            this.childName = childName;
            this.CHANNEL_ID = CHANNEL_ID;
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        }
        catch (Exception e){
            e.printStackTrace();
        }
        NOTIFICATION_ID = getID();

        final Intent closeButton = new Intent(context, CloseButton.class);
        closeButton.putExtra("Id", NOTIFICATION_ID);
        closeButton.setAction("Close_Button");
        /*IntentFilter intentFilterClose = new IntentFilter();
        intentFilterClose.addAction("Close_Button");
        intentFilterClose.addCategory("com.example.babymonitorv2");
        context.registerReceiver(new CloseButton(),intentFilterClose);*/
        closeButton.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingCloseIntent = PendingIntent.getBroadcast(context, 0, closeButton, PendingIntent.FLAG_UPDATE_CURRENT);

        final Intent listenButton = new Intent(context, ListenButton.class);
        listenButton.putExtra("Id",NOTIFICATION_ID);
        listenButton.setAction("Listen_Button");
        /*IntentFilter intentFilterListen = new IntentFilter();
        intentFilterListen.addAction("Listen_Button");
        intentFilterListen.addCategory("com.example.babymonitorv2");
        context.registerReceiver(new ListenButton(),intentFilterListen);*/
        listenButton.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingListenIntent = PendingIntent.getBroadcast(context, 0, listenButton, PendingIntent.FLAG_UPDATE_CURRENT);

        final RemoteViews remoteViews = new RemoteViews(packageName, R.layout.servis_notification);
        remoteViews.setOnClickPendingIntent(R.id.serviceButtonExit, pendingCloseIntent);
        remoteViews.setOnClickPendingIntent(R.id.listenToggleButton, pendingListenIntent);
        remoteViews.setInt(R.id.logoImageView, "setImageResource", R.mipmap.ic_baby_sleep);
        remoteViews.setTextViewText(R.id.titleTextView, childName);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context,CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_baby_baby)
                .setCustomContentView(remoteViews)
                .setCustomBigContentView(remoteViews);

        notification = builder.build();

        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification);

        createTimer();

        Thread listen = new Thread(new Runnable() {
            WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
            AudioStreamer audioStreamer = AudioStreamer.getInstance();
            @Override
            public void run() {
                while (true) {
                    try {
                        if(dataInputStream.available() > 0) {
                            if(isVoiceButtonSet) {
                                byte reader[] = new byte[2052];
                                dataInputStream.readFully(reader, 0, reader.length);
                                short audioBuffer[] = byte2short(reader);
                                audioStreamer.streamAudio(audioBuffer);
                                if(reader[reader.length - 2] == 3){
                                    if(!isCrying) {
                                        //stopForeground(true);
                                        remoteViews.setTextViewText(R.id.statusTextView, "Bebek Ağlıyor");
                                        remoteViews.setInt(R.id.logoImageView, "setImageResource", R.mipmap.ic_baby_cry);
                                        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification);
                                        Vibrator vibrator = (Vibrator)context.getSystemService(VIBRATOR_SERVICE);
                                        vibrator.vibrate(2000);
                                        isCrying = true;

                                    }
                                    // timer tut
                                }
                                else{
                                    if(isCrying && reader[reader.length - 1] != 1) {
                                        remoteViews.setTextViewText(R.id.statusTextView, "Bebek Ağlamıyor");
                                        remoteViews.setInt(R.id.logoImageView, "setImageResource", R.mipmap.ic_baby_sleep);
                                        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification);
                                        isCrying = false;
                                    }
                                }
                                if(reader[reader.length - 1] == 1){
                                    isVoiceButtonSet = false;
                                    trigger = false;
                                    remoteViews.setInt(R.id.listenToggleButton,"setBackgroundResource",R.drawable.play_button);
                                    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification);
                                    audioStreamer.stopPlaying();
                                }
                                continue;
                            }
                            else{
                                int read = dataInputStream.read();
                                if(read == 3){
                                    if(!isCrying) {
                                        remoteViews.setTextViewText(R.id.statusTextView, "Bebek Ağlıyor");
                                        remoteViews.setInt(R.id.logoImageView, "setImageResource", R.mipmap.ic_baby_cry);
                                        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification);
                                        Vibrator vibrator = (Vibrator)context.getSystemService(VIBRATOR_SERVICE);
                                        vibrator.vibrate(2000);
                                        isCrying = true;
                                    }
                                }
                                else{
                                    if(isCrying && read != 0) {
                                        remoteViews.setTextViewText(R.id.statusTextView, "Bebek Ağlamıyor");
                                        remoteViews.setInt(R.id.logoImageView, "setImageResource", R.mipmap.ic_baby_sleep);
                                        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification);
                                        isCrying = false;
                                    }
                                }
                                if(read == 0) {
                                    isVoiceButtonSet = true;
                                    trigger = false;
                                    remoteViews.setInt(R.id.listenToggleButton,"setBackgroundResource",R.drawable.pause_button);
                                    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification);
                                    audioStreamer.startPlaying();
                                }
                            }
                        }
                    }
                    catch(Exception e){
                        try {
                            if(audioStreamer.isPlaying()){
                                isVoiceButtonSet = false;
                                remoteViews.setInt(R.id.listenToggleButton,"setBackgroundResource",R.drawable.play_button);
                                NotificationManagerCompat.from(context);
                                audioStreamer.stopPlaying();
                            }
                        }
                        catch (Exception ex){
                            ex.printStackTrace();
                        }
                        finally {
                            e.printStackTrace();
                            break;
                        }
                    }
                }
            }
        });
        listen.start();

        currentState = State.RUNNING;
    }

    private short[] byte2short(byte[] bData) {
        int byteArrsize = bData.length;
        short[] shortArr = new short[byteArrsize/2 - 2];
        for (int i = 0; i < byteArrsize - 4; i+=2) {
            shortArr[i/2] = bData[i];
            if(shortArr[i/2] < 0)
                shortArr[i/2] += 256;
            short val = bData[i+1];
            if(val < 0)
                shortArr[i/2] += 256;
            shortArr[i/2] |= (val << 8);
        }
        return shortArr;

    }

    private void createTimer(){
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    dataOutputStream.write(2);
                }
                catch (Exception e){
                    e.printStackTrace();
                    //Exception olusursa bitir.
                    //Current Activity değil, Baska bir activityde kostur

                    /*context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
                            builder.setTitle("Hata")
                                    .setMessage("Karşı cihaz ile olan bağlantınız koptu. Lütfen Tekrar deneyin")
                                    .setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    });
                            builder.create().show();
                            //currentActivity.finish();
                        }
                    });*/
                    try {
                        Log.d(TAG, "run: closing socket");
                        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
                        currentState = State.FINISHED;
                        timer.cancel();
                        socket.close();
                        EbeveynDinlemeActivity.EbeveynDinlemeServis.checkChildArray();
                        createErrorNotification();
                    }
                    catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
            }
        },1000, 3000);
    }

    public Notification getNotification(){
        return notification;
    }

    public int getNOTIFICATION_ID(){
        return NOTIFICATION_ID;
    }

    public static int getID()
    {
        return atomicInt.getAndIncrement();
    }

    public static int getErrorID()
    {
        return atomicErrorInt.getAndIncrement();
    }

    private void createErrorNotification(){
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context,CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_baby_baby)
                .setContentTitle("Baby Monitor - Hata")
                .setContentText(childName + " isimli çocuğu dinleyen cihaz ile bağlantı koptu.")
                .setPriority(Notification.PRIORITY_DEFAULT);

        NotificationManagerCompat.from(context).notify(getErrorID(), builder.build());
    }

    public void onReceiveClose(Context context, Intent in){
        if(isVoiceButtonSet){
            Toast.makeText(context, "Lütfen dinleme yapmayı bırakın", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
            socket.close();
            timer.cancel();
            currentState = State.FINISHED;
            EbeveynDinlemeActivity.EbeveynDinlemeServis.checkChildArray();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void onReceiveListen(Context context, Intent in){
        if(trigger){
            if(isVoiceButtonSet){
                Toast.makeText(context,"Dinleme kapatılıyor.",Toast.LENGTH_LONG).show();

            }
            if(!isVoiceButtonSet){
                Toast.makeText(context, "Dinleme açılıyor.",Toast.LENGTH_LONG).show();
            }
            return;
        }
                    /*if(!isServiceOpen){
                        Toast.makeText(EbeveynDinlemeServis.this, "Bebek telefonunu dinleyebilmek için servisi aktifleştirmelisiniz.", Toast.LENGTH_LONG).show();
                        return;
                    }*/
        if(socket.isClosed()){
            Toast.makeText(context, "Bağlantınız koptu. Lütfen ebeveyn telefonu ile tekrar bağlantı kurun", Toast.LENGTH_LONG).show();
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
            timer.cancel();
            currentState = State.FINISHED;
            createErrorNotification();

            EbeveynDinlemeActivity.EbeveynDinlemeServis.checkChildArray();
            return;
        }
        trigger = true;
        try {
            if (isVoiceButtonSet) {
                dataOutputStream.write(1);
            } else {
                dataOutputStream.write(0);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


}
