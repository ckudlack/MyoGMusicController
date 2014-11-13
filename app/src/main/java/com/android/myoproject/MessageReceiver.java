package com.android.myoproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MessageReceiver extends BroadcastReceiver {

    public MessageReceiver() {
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