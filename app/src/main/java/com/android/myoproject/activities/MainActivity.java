package com.android.myoproject.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.myoproject.BusEvent;
import com.android.myoproject.Constants;
import com.android.myoproject.application.MyoApplication;
import com.android.myoproject.R;
import com.android.myoproject.background.MusicControllerService;
import com.squareup.otto.Subscribe;
import com.thalmic.myo.Arm;

public class MainActivity extends Activity {

    private ImageView gestureImage;
    private TextView connectedView;
    private TextView syncedView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MyoApplication.bus.register(this);

        gestureImage = (ImageView) findViewById(R.id.gesture_image);
        connectedView = (TextView) findViewById(R.id.connection_state);
        syncedView = (TextView) findViewById(R.id.sync_state);

        SharedPreferences preferences = getSharedPreferences("PREFS", MODE_PRIVATE);

        boolean isConnected = preferences.getBoolean(Constants.CONNECTION_KEY, false);
        connectedView.setCompoundDrawables(null, null, getDrawable(isConnected ? R.drawable.green_circle : R.drawable.red_circle), null);

        boolean isSynced = preferences.getBoolean(Constants.SYNC_KEY, false);
        syncedView.setCompoundDrawables(null, null, getDrawable(isSynced ? R.drawable.green_circle : R.drawable.red_circle), null);

        if (!preferences.getBoolean(Constants.NOTIFICATION_ACTIVE, false)) {
            preferences.edit().putBoolean(Constants.NOTIFICATION_ACTIVE, true).apply();
            startService(new Intent(this, MusicControllerService.class));
        }
    }

    @Subscribe
    public void gestureUpdated(BusEvent.GestureUpdatedEvent event) {
        int resource = 0;
        Arm arm = event.getArm();

        switch (event.getPose()) {
            case FIST:
                resource = arm == Arm.LEFT ? R.drawable.left_fist : R.drawable.right_fist;
                break;
            case FINGERS_SPREAD:
                resource = arm == Arm.LEFT ? R.drawable.left_spread_fingers : R.drawable.right_spread_fingers;
                break;
            case WAVE_IN:
                resource = arm == Arm.LEFT ? R.drawable.left_wave_right : R.drawable.right_wave_left;
                break;
            case WAVE_OUT:
                resource = arm == Arm.LEFT ? R.drawable.left_wave_left : R.drawable.right_wave_right;
                break;
            case THUMB_TO_PINKY:
                resource = arm == Arm.LEFT ? R.drawable.left_pinky_thumb : R.drawable.right_pinky_thumb;
                break;
            case REST:
                resource = R.drawable.blank_circle;
                break;
        }
        gestureImage.setImageResource(resource);
    }

    @Subscribe
    public void connectionStateUpdated(BusEvent.MyoConnectionStatusEvent event) {
        connectedView.setCompoundDrawables(null, null, getDrawable(event.isConnected() ? R.drawable.green_circle : R.drawable.red_circle), null);
    }

    @Subscribe
    public void syncStateUpdated(BusEvent.MyoSyncStatusEvent event) {
        syncedView.setCompoundDrawables(null, null, getDrawable(event.isSynced() ? R.drawable.green_circle : R.drawable.red_circle), null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyoApplication.bus.register(this);
    }

    @Override
    protected void onPause() {
        MyoApplication.bus.unregister(this);
        super.onPause();
    }
}
