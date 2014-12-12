package com.android.myoproject.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
    private Button scanButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MyoApplication.bus.register(this);

        gestureImage = (ImageView) findViewById(R.id.gesture_image);
        connectedView = (TextView) findViewById(R.id.connection_state);
        syncedView = (TextView) findViewById(R.id.sync_state);
        scanButton = (Button) findViewById(R.id.scan);

        final SharedPreferences preferences = getSharedPreferences(Constants.PREFERENCES, MODE_PRIVATE);

        boolean isConnected = preferences.getBoolean(Constants.CONNECTION_KEY, false);
        connectedView.setCompoundDrawablesWithIntrinsicBounds(0, 0, isConnected ? R.drawable.green_circle : R.drawable.red_circle, 0);

        boolean isSynced = preferences.getBoolean(Constants.SYNC_KEY, false);
        syncedView.setCompoundDrawablesWithIntrinsicBounds(0, 0, isSynced ? R.drawable.green_circle : R.drawable.red_circle, 0);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!preferences.getBoolean(Constants.NOTIFICATION_ACTIVE, false)) {
                    preferences.edit().putBoolean(Constants.NOTIFICATION_ACTIVE, true).apply();
                    startService(new Intent(MainActivity.this, MusicControllerService.class));
                } else {
                    Toast.makeText(MainActivity.this, "Myo Service already running. Hold button to override", Toast.LENGTH_LONG).show();
                }
            }
        });

        scanButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                preferences.edit().putBoolean(Constants.NOTIFICATION_ACTIVE, true).apply();
                startService(new Intent(MainActivity.this, MusicControllerService.class));
                return true;
            }
        });
    }

    private void goToNotificationSettingsMenu() {
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        startActivityForResult(intent, 1);
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
            case DOUBLE_TAP:
                resource = arm == Arm.LEFT ? R.drawable.left_double_tap : R.drawable.right_double_tap;
                break;
            case REST:
                resource = R.drawable.transparent_circle;
                break;
        }
        gestureImage.setImageResource(resource);
    }

    @Subscribe
    public void connectionStateUpdated(BusEvent.MyoConnectionStatusEvent event) {
        connectedView.setCompoundDrawablesWithIntrinsicBounds(0, 0, event.isConnected() ? R.drawable.green_circle : R.drawable.red_circle, 0);
    }

    @Subscribe
    public void syncStateUpdated(BusEvent.MyoSyncStatusEvent event) {
        syncedView.setCompoundDrawablesWithIntrinsicBounds(0, 0, event.isSynced() ? R.drawable.green_circle : R.drawable.red_circle, 0);
    }

    @Subscribe
    public void userNeedsPermission(BusEvent.UserNeedsPermissionEvent event) {
        goToNotificationSettingsMenu();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MyoApplication.bus.post(new BusEvent.RestartControllerEvent());
    }

    @Override
    protected void onStart() {
        super.onStart();
        MyoApplication.bus.register(this);
    }

    @Override
    protected void onStop() {
        MyoApplication.bus.unregister(this);
        super.onStop();
    }
}
