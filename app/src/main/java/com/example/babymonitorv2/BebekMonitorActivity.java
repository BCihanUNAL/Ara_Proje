package com.example.babymonitorv2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Cihan
 */

public class BebekMonitorActivity extends AppCompatActivity {

    static{
        System.loadLibrary("tensorflow_inference");
    }

    private static final String MODEL_FILE = "file:///android_asset/baby_model.pb";
    private static final String INPUT_NODE = "input_1";
    private static final String OUTPUT_NODE = "sequential/output/Softmax";//
    private static final String TAG = "BebekMonitorActivity";
    private static Socket socket;
    private static final int RECORDER_SAMPLERATE = 16000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private boolean isListening = false;
    private BebekMonitorActivity bebekMonitorActivity;
    private static Timer timer;
    public static short processData[];
    private static int offset;
    private static int[] INPUT_SHAPE = {13 * 157};
    private boolean isCrying = false;
    private static boolean isCreated = false;
    private static TensorFlowInferenceInterface tensorFlowInferenceInterface;
    private static TelephonyManager telephonyManager;

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bebek_monitor);
        if(savedInstanceState != null && savedInstanceState.containsKey("iscrying")){
            isListening = savedInstanceState.getBoolean("islistening");
            isRecording = savedInstanceState.getBoolean("isrecording");
            isCrying = savedInstanceState.getBoolean("iscrying");
        }
        bebekMonitorActivity = this;

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        if(!isCrying) {
            bebekMonitorActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((ImageView) findViewById(R.id.babyStatusImageView)).setImageResource(R.drawable.baby_sleep);
                    ((TextView) findViewById(R.id.babyStatusTextView)).setText("Bebeğiniz Ağlamıyor");
                }
            });
        }
        if(isCrying) {
            bebekMonitorActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((ImageView) findViewById(R.id.babyStatusImageView)).setImageResource(R.drawable.baby_cry);
                    ((TextView) findViewById(R.id.babyStatusTextView)).setText("Bebeğiniz Ağlıyor");
                }
            });
        }
        if(!isCreated) {
            isCreated = true;
            telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            processData = new short[RECORDER_SAMPLERATE * 5];
            offset = 0;
            tensorFlowInferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE);
            socket = BebekServisKayitActivity.getBabySocket();

            final WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            Log.d(TAG, "onCreate: Socket = " + socket.getInetAddress().toString());
            if (!isRecording) {
                try {
                    socket.setSoTimeout(10000);
                    startRecording();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startRecording() {

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, 2048);

        recorder.startRecording();
        isListening = false;
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                try {
                    writeDataToBuffer();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        System.arraycopy(sData,0,processData,offset,Math.min(sData.length, processData.length - offset));
        if(processData.length - offset > sData.length)
            offset += sData.length;
        else{
            detectBabyCry();
            offset = 0;
        }
        byte[] bytes = new byte[shortArrsize * 2 + 4];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
        }
        for(int i = 2 * shortArrsize; i < 2 * shortArrsize + 4; i++ ){
            bytes[i] = 0;
        }
        return bytes;
    }

    private void detectBabyCry(){
        Thread detectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: isleniyor");
                double doubleMFCC[][] = FeatureExtraction.process(processData);
                Log.d(TAG, "run: " + processData[1] + " " + processData[2] + " " + processData[3] + " " + processData[4] + " " + processData[5] + " " + processData[6] + " " + processData[7] + " " + processData[8] + " " + processData[9] + " " + processData[10] + " ");
                float MFCC[] = new float[doubleMFCC.length * doubleMFCC[0].length];
                for (int i = 0; i < doubleMFCC.length; i++) {
                    for (int j = 0; j < doubleMFCC[0].length; j++) {
                        MFCC[i * doubleMFCC[0].length + j] = (float) (doubleMFCC[i][j]);
                    }
                }
                tensorFlowInferenceInterface.feed(INPUT_NODE, MFCC, 1, INPUT_SHAPE[0]);

                tensorFlowInferenceInterface.run(new String[]{OUTPUT_NODE});
                float results[] = {0, 0};
                tensorFlowInferenceInterface.fetch(OUTPUT_NODE, results);
                if(results[1] > results[0]){
                    Log.d(TAG, "run: bebek agliyor " + results[0] + " " + results[1] + " " + isCrying);
                    if(!isCrying) {
                        isCrying = true;
                        bebekMonitorActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((ImageView) findViewById(R.id.babyStatusImageView)).setImageResource(R.drawable.baby_cry);
                                ((TextView) findViewById(R.id.babyStatusTextView)).setText("Bebeğiniz Ağlıyor");
                            }
                        });
                    }
                }
                else{
                    Log.d(TAG, "run: bebek aglamiyor " + results[0] + " " + results[1] + " " + isCrying);
                    if(isCrying) {
                        isCrying = false;
                        bebekMonitorActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((ImageView) findViewById(R.id.babyStatusImageView)).setImageResource(R.drawable.baby_sleep);
                                ((TextView) findViewById(R.id.babyStatusTextView)).setText("Bebeğiniz Ağlamıyor");
                            }
                        });
                    }
                }
            }
        });
        detectionThread.start();
    }

    private void writeDataToBuffer() throws IOException{
        short sData[] = new short[1024];
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        final DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
        if(socket == null){
            Log.d(TAG, "writeAudioDataToBuffer: Socket is null");
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(!isListening){
                    try {
                        if(isCrying)
                            dataOutputStream.write(3);
                        else
                            dataOutputStream.write(2);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        bebekMonitorActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                BebekServisKayitActivity.showErrorOnReturn();
                                BebekMonitorActivity.this.finish();
                                timer.cancel();
                            }
                        });
                    }
                }
            }
        }, 1000, 3000);

        while (true) {
            if(!socket.isConnected()){
                break;
            }
            try {
                byte b = 127;
                if(dataInputStream.available() > 0){
                    b = dataInputStream.readByte();
                }
                recorder.read(sData, 0, sData.length);

                byte bData[] = short2byte(sData);

                if(isListening) {
                    if(isCrying){
                        bData[bData.length - 2] = 3;
                    }
                    if(b == 1 || telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE){
                        isListening = false;
                        bData[bData.length - 1] = 1;
                    }
                    dataOutputStream.write(bData, 0, bData.length);
                }
                else{
                    if(b == 0){
                        Log.d(TAG, "writeDataToBuffer: sending");
                        isListening = true;
                        if(isCrying){
                            dataOutputStream.write(3);
                        }
                        dataOutputStream.write(0);
                    }
                }
            }
            catch (Exception e){
                Log.d(TAG, "writeAudioDataToBuffer: hata cikti");
                e.printStackTrace();
                break;
            }

        }
        bebekMonitorActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    stopRecording();
                    if (!socket.isClosed()) {
                        socket.close();
                    }
                    bebekMonitorActivity.finish();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void stopRecording() {
        if (null != recorder) {
            isListening = false;
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isListening = savedInstanceState.getBoolean("islistening");
        isRecording = savedInstanceState.getBoolean("isrecording");
        isCrying = savedInstanceState.getBoolean("iscrying");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("islistening",isListening);
        outState.putBoolean("isrecording",isRecording);
        outState.putBoolean("iscrying",isCrying);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        if(!isChangingConfigurations()) {
            try {
                stopRecording();
                timer.cancel();
                isCreated = false;
                //new DataOutputStream(socket.getOutputStream()).writeByte(10);
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();

    }

}