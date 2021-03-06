package com.example.babymonitorv2;

import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * @author Cihan
 */

public class AudioStreamer {
    private static AudioStreamer instance = null;
    private static final String TAG = "AudioStreamer";
    private AudioTrack audioTrack;
    private boolean isPlaying = false;

    public AudioStreamer(){
        Log.i(TAG, "Setting up stream");

        final int frequency = 16000;
        final int channelConfiguration = AudioFormat.CHANNEL_OUT_MONO;
        final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        final int bufferSize = AudioTrack.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                frequency,
                channelConfiguration,
                audioEncoding,
                bufferSize,
                AudioTrack.MODE_STREAM);

        audioTrack.setVolume(1.0f);

        //caller.setVolumeControlStream(AudioAttributes.USAGE_VOICE_COMMUNICATION);
    }

    public static AudioStreamer getInstance(){
        if(instance == null)
            instance = new AudioStreamer();
        return instance;
    }


    public void streamAudio(short audioBuffer[]){
        audioTrack.write(audioBuffer, 0, audioBuffer.length - 4);
    }

    public void stopPlaying(){
        if(audioTrack != null) {
            audioTrack.stop();
            isPlaying = false;
        }
    }

    public void startPlaying(){
        if(audioTrack != null){
            audioTrack.play();
            isPlaying = true;
        }
    }

    public boolean isPlaying(){
        return isPlaying;
    }
}