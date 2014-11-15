package com.android.myoproject.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.android.myoproject.BusEvent;
import com.android.myoproject.application.MyoApplication;

public class PlaybackStateReceiver extends BroadcastReceiver {

    public PlaybackStateReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isPlaying = false;

        if (intent.hasExtra("playing")) {
            isPlaying = intent.getBooleanExtra("playing", false);
        }

        MyoApplication.bus.post(new BusEvent.PlaybackUpdatedEvent(isPlaying));
    }
}