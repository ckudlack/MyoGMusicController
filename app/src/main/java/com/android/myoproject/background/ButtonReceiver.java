package com.android.myoproject.background;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.android.myoproject.BusEvent;
import com.android.myoproject.Constants;
import com.android.myoproject.application.MyoApplication;

public class ButtonReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId = intent.getIntExtra(Constants.NOTIFICATION_ID, 0);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(notificationId);

        MyoApplication.bus.post(new BusEvent.DestroyServiceEvent());
    }
}
