package com.example.babymonitorv2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Cihan
 */

public class EbeveynDinlemeActivity extends AppCompatActivity {
    private static final String TAG = "EbeveynDinlemeActivity";
    public static final String CHANNEL_ID = "EbeveynServiceChannel";
    private static Socket socket;
    private static boolean isVoiceButtonSet = false;
    private static boolean trigger = false;
    private static boolean isServiceOpen = false;
    private static Context context;
    private static Intent intent;
    private static Activity currentActivity;
    private static DataInputStream dataInputStream;
    private static DataOutputStream dataOutputStream;
    private static NotificationManager notificationManager;
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
            socket = EbeveynServisKayitActivity.getParentSocket();

            childName = getIntent().getStringExtra("ChildName");
            currentActivity = this;
            try {
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                socket.setSoTimeout(10000);
            }
            catch(Exception e){
                e.printStackTrace();
            }

            Log.d(TAG, "onCreate: socket = " + socket.getInetAddress().toString());
            context = this;
            createNotificationChannel();

            intent = new Intent(EbeveynDinlemeActivity.this, EbeveynDinlemeServis.class);
            startService(intent);
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

    public static void initBooleanVariables(){
        trigger = false;
        isServiceOpen = false;
        isVoiceButtonSet = false;
    }

    public static Socket getParentSocket(){
        return EbeveynServisKayitActivity.getParentSocket();
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
        super.onDestroy();
    }


    public static class EbeveynDinlemeServis extends Service {
        private static final String TAG = "EbeveynDinlemeServis";
        public static final String CHANNEL_ID = "EbeveynServiceChannel";
        private static int NOTIFICATION_ID;
        private Thread listen;
        private Timer timer;
        private Notification notification;
        private boolean isCrying;
        private static Button listenButton;
        private static Button serviceButton;


        public EbeveynDinlemeServis(){
            super();
        }

        @Override
        public void onCreate() {
            super.onCreate();
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            isCrying = false;
            isServiceOpen = true;

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
                        currentActivity.runOnUiThread(new Runnable() {
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
                                stopForeground(true);
                                stopSelf();
                                timer.cancel();
                                currentActivity.finish();
                            }
                        });
                    }
                }
            },1000, 3000);


        }

        private void setButtons(View view){
            LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View dummyView = layoutInflater.inflate(R.layout.servis_notification, null);

            serviceButton = (Button)view.findViewById(R.id.serviceButtonExit);
            listenButton = (Button)view.findViewById(R.id.listenToggleButton);


            serviceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isVoiceButtonSet){
                        Toast.makeText(EbeveynDinlemeServis.this, "Lütfen dinleme yapmayı bırakın", Toast.LENGTH_LONG).show();
                        return;
                    }
                    stopForeground(true);
                    stopSelf();
                }
            });

            listenButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: pressed listen button");
                    if(trigger){
                        if(isVoiceButtonSet){
                            Toast.makeText(EbeveynDinlemeServis.this,"Dinleme kapatılıyor.",Toast.LENGTH_LONG).show();
                        }
                        if(!isVoiceButtonSet){
                            Toast.makeText(EbeveynDinlemeServis.this, "Dinleme açılıyor.",Toast.LENGTH_LONG).show();
                        }
                        return;
                    }
                    /*if(!isServiceOpen){
                        Toast.makeText(EbeveynDinlemeServis.this, "Bebek telefonunu dinleyebilmek için servisi aktifleştirmelisiniz.", Toast.LENGTH_LONG).show();
                        return;
                    }*/
                    if(socket.isClosed()){
                        Toast.makeText(EbeveynDinlemeServis.this, "Bağlantınız koptu. Lütfen ebeveyn telefonu ile tekrar bağlantı kurun", Toast.LENGTH_LONG).show();
                        stopForeground(true);
                        stopSelf();
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
            });

        }

        private void initialize(){
            final Intent closeButton = new Intent(this, CloseButton.class);
            closeButton.setAction("Close_Button");
            closeButton.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingCloseIntent = PendingIntent.getBroadcast(this, 0, closeButton, 0);

            Intent listenButton = new Intent(this, ListenButton.class);
            listenButton.setAction("Listen_Button");
            closeButton.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingListenIntent = PendingIntent.getBroadcast(this, 0, listenButton, 0);

            final RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.servis_notification);
            remoteViews.setOnClickPendingIntent(R.id.serviceButtonExit, pendingCloseIntent);
            remoteViews.setOnClickPendingIntent(R.id.listenToggleButton, pendingListenIntent);
            remoteViews.setTextViewText(R.id.titleTextView, childName);

            final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setCustomContentView(remoteViews)
                    .setCustomBigContentView(remoteViews);

            notification = builder.build();

            notification.flags |= Notification.FLAG_ONGOING_EVENT;

            NOTIFICATION_ID = NotificationID.getID();
            startForeground(NOTIFICATION_ID, notification);

            listen = new Thread(new Runnable() {
                WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                AudioStreamer audioStreamer = AudioStreamer.getInstance(currentActivity);
                @Override
                public void run() {
                    while (true) {
                        try {
                            if(dataInputStream.available() > 0) {
                                if(isVoiceButtonSet) {
                                    byte reader[] = new byte[2052];
                                    dataInputStream.readFully(reader, 0, reader.length);
                                    Log.d(TAG, "run: " + reader[0]);
                                    short audioBuffer[] = byte2short(reader);
                                    audioStreamer.streamAudio(audioBuffer);
                                    if(reader[reader.length - 2] == 3){
                                        if(!isCrying) {
                                            //stopForeground(true);
                                            remoteViews.setTextViewText(R.id.statusTextView, "Bebek Ağlıyor");
                                            remoteViews.setInt(R.id.logoImageView, "setImageResource", R.mipmap.ic_baby_cry);
                                            NotificationManagerCompat.from(EbeveynDinlemeServis.this).notify(NOTIFICATION_ID, notification);
                                            Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                                            vibrator.vibrate(2000);
                                            isCrying = true;

                                        }
                                        // timer tut
                                    }
                                    else{
                                        if(isCrying && reader[reader.length - 1] != 1) {
                                            remoteViews.setTextViewText(R.id.statusTextView, "Bebek Ağlamıyor");
                                            remoteViews.setInt(R.id.logoImageView, "setImageResource", R.mipmap.ic_baby_sleep);
                                            NotificationManagerCompat.from(EbeveynDinlemeServis.this).notify(NOTIFICATION_ID, notification);
                                            isCrying = false;
                                        }
                                    }
                                    if(reader[reader.length - 1] == 1){
                                        isVoiceButtonSet = false;
                                        trigger = false;
                                        remoteViews.setInt(R.id.listenToggleButton,"setBackgroundResource",R.drawable.play_button);
                                        NotificationManagerCompat.from(EbeveynDinlemeServis.this).notify(NOTIFICATION_ID, notification);
                                        audioStreamer.stopPlaying();
                                    }
                                    continue;
                                }
                                else{
                                    int read = dataInputStream.read();
                                    Log.d(TAG, "run: read = " + read);
                                    if(read == 3){
                                        if(!isCrying) {
                                            remoteViews.setTextViewText(R.id.statusTextView, "Bebek Ağlıyor");
                                            remoteViews.setInt(R.id.logoImageView, "setImageResource", R.mipmap.ic_baby_cry);
                                            NotificationManagerCompat.from(EbeveynDinlemeServis.this).notify(NOTIFICATION_ID, notification);
                                            isCrying = true;
                                        }
                                    }
                                    else{
                                        if(isCrying && read != 0) {
                                            remoteViews.setTextViewText(R.id.statusTextView, "Bebek Ağlamıyor");
                                            remoteViews.setInt(R.id.logoImageView, "setImageResource", R.mipmap.ic_baby_sleep);
                                            NotificationManagerCompat.from(EbeveynDinlemeServis.this).notify(NOTIFICATION_ID, notification);
                                            isCrying = false;
                                        }
                                    }
                                    if(read == 0) {
                                        isVoiceButtonSet = true;
                                        trigger = false;
                                        remoteViews.setInt(R.id.listenToggleButton,"setBackgroundResource",R.drawable.pause_button);
                                        NotificationManagerCompat.from(EbeveynDinlemeServis.this).notify(NOTIFICATION_ID, notification);
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
                                    NotificationManagerCompat.from(EbeveynDinlemeServis.this);
                                    audioStreamer.stopPlaying();
                                }
                            }
                            catch (Exception ex){
                                ex.printStackTrace();
                            }
                            finally {
                                e.printStackTrace();
                                Log.d(TAG, "run: cihan");
                                break;
                            }
                        }
                    }
                }
            });
            listen.start();
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            initialize();
            return START_NOT_STICKY;
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

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onDestroy() {
            try {
                //??????????
                trigger = false;
                isVoiceButtonSet = false;
                try {
                    socket.close();//Notifikasyona gore ayarlanacak
                    timer.cancel();
                    isVoiceButtonSet = false;
                    trigger = false;

                    stopService(intent);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            catch (Exception e){
                e.printStackTrace();
            }
            super.onDestroy();
        }

        public static class CloseButton extends BroadcastReceiver{
            public CloseButton(){
                super();
            }
            @Override
            public void onReceive(Context context, Intent in) {
                if(isVoiceButtonSet){
                    Toast.makeText(context, "Lütfen dinleme yapmayı bırakın", Toast.LENGTH_LONG).show();
                    return;
                }
                context.stopService(intent);
            }
        }

        public static class ListenButton extends BroadcastReceiver{
            public ListenButton(){
                super();
            }
            @Override
            public void onReceive(Context context, Intent in) {
                Log.d(TAG, "onClick: pressed listen button");
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
                    context.stopService(intent);
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

        static class NotificationID {
            private final static AtomicInteger c = new AtomicInteger(0);
            public static int getID() {
                return c.incrementAndGet();
            }
        }

    }


}