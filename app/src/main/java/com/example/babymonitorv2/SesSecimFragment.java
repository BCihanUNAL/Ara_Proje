package com.example.babymonitorv2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class SesSecimFragment extends DialogFragment {
    private int position = 0;
    private MediaPlayer mp = null;
    public static SesSecimFragment getInstance() {
        SesSecimFragment sesSecimFragment = new SesSecimFragment();
        return sesSecimFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final RingtoneManager manager = new RingtoneManager(getActivity());
        manager.setType(RingtoneManager.TYPE_NOTIFICATION);
        final Cursor cursor = manager.getCursor();
        ArrayList<String> titleList = new ArrayList<>();
        ArrayList<Uri> ringtoneURIList = new ArrayList<>();

        while (cursor.moveToNext()) {
            String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
            titleList.add(title);
            Uri ringtoneURI = manager.getRingtoneUri(cursor.getPosition());
            ringtoneURIList.add(ringtoneURI);
            // Do something with the title and the URI of ringtone
        }
        String[] titleArray = new String[titleList.size()];
        final Uri[] uriArray = new Uri[ringtoneURIList.size()];
        titleList.toArray(titleArray);
        ringtoneURIList.toArray(uriArray);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                                      .setTitle("Ses Dosyası Seçiniz")
                                      .setSingleChoiceItems(titleArray,0,
                                              new DialogInterface.OnClickListener() {
                                                  @Override
                                                  public void onClick(DialogInterface dialog, int which) {
                                                      Uri notificationRingtone = manager.getRingtoneUri(which);

                                                      if(mp != null && mp.isPlaying())
                                                          mp.stop();
                                                      mp = MediaPlayer.create(getContext(), notificationRingtone);
                                                      mp.start();
                                                      position = which;
                                                  }
                                              })
                                      .setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
                                          @Override
                                          public void onClick(DialogInterface dialog, int which) {
                                                EbeveynServisKayitActivity.setSoundFile(uriArray[position]);
                                                dialog.dismiss();
                                          }
                                      })
                                      .setNegativeButton("İptal", new DialogInterface.OnClickListener() {
                                          @Override
                                          public void onClick(DialogInterface dialog, int which) {
                                               dialog.dismiss();
                                          }
                                      });
        return builder.create();
        //return super.onCreateDialog(savedInstanceState);
    }
}
