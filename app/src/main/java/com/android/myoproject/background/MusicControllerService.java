package com.android.myoproject.background;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.myoproject.BusEvent;
import com.android.myoproject.Constants;
import com.android.myoproject.R;
import com.android.myoproject.application.MyoApplication;
import com.android.myoproject.callbacks.DeviceCallback;
import com.android.myoproject.custommyo.MyoDeviceListener;
import com.squareup.otto.Subscribe;
import com.thalmic.myo.Arm;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;

import java.util.List;

public class MusicControllerService extends Service implements DeviceCallback {

    private static final float ROLL_THRESHOLD = 1.5f;
    private static final float PITCH_THRESHOLD = 0.0f;
    private static final float YAW_THRESHOLD = 1.5f;

    private static final int NOTIFICATION_ID = 50990;

    private boolean blockEverything = false;

    private double currentRoll = 0;
    private double currentYaw = 0;
    private double currentPitch = 0;

    private boolean fistMade = false;
    private boolean lockToggleMode = false;

    private double referenceRoll = 0;
    private double referenceYaw = 0;
    private double referencePitch = 0;

    private Myo currentMyo;

    private AudioManager audioManager;
    private MyoDeviceListener deviceListener;

    private MediaController controller;

    private int playbackState;

    private MediaController.Callback callback;
    private SharedPreferences preferences;

    private boolean isRolling = false;
    private boolean isYawing = false;

    private boolean waitingForDT = false;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        MyoApplication.bus.register(this);

        initPlaybackState();

        preferences = getSharedPreferences(Constants.PREFERENCES, MODE_PRIVATE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        deviceListener = new MyoDeviceListener(this, this);

        Hub hub = Hub.getInstance();
        if (!hub.init(this, getPackageName())) {
            Log.e("TAG", "Could not init Hub");
            this.stopSelf();
            return;
        }

        hub.addListener(deviceListener);
        hub.attachByMacAddress("FB:75:87:D3:FD:65");

        //Create an Intent for the BroadcastReceiver
        Intent buttonIntent = new Intent(this, ButtonReceiver.class);
        buttonIntent.putExtra("notificationId", NOTIFICATION_ID);

        //Create the PendingIntent
        PendingIntent btPendingIntent = PendingIntent.getBroadcast(this, 0, buttonIntent, 0);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Myo")
                        .setOngoing(true)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentText("Running")
                        .addAction(R.drawable.ic_action_cancel, "Close", btPendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());

        createCallback();

        try {
            createMediaController();
        } catch (SecurityException e) {
            MyoApplication.bus.post(new BusEvent.UserNeedsPermissionEvent());
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void initPlaybackState() {
        playbackState = PlaybackState.STATE_PLAYING;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void createCallback() {
        callback = new MediaController.Callback() {
            @Override
            public void onPlaybackStateChanged(PlaybackState state) {
                playbackState = state.getState();
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void createMediaController() throws SecurityException {
        ComponentName notificationListener = new ComponentName(getPackageName(), NotificationListener.class.getName());

        MediaSessionManager sessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        sessionManager.addOnActiveSessionsChangedListener(new MediaSessionManager.OnActiveSessionsChangedListener() {
            @Override
            public void onActiveSessionsChanged(List<MediaController> controllers) {
                getFirstSession(controllers);
            }
        }, notificationListener);

        List<MediaController> controllers = sessionManager.getActiveSessions(notificationListener);
        getFirstSession(controllers);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void getFirstSession(List<MediaController> activeSessions) {
        if (activeSessions.size() > 0) {
            controller = activeSessions.get(0);
            controller.registerCallback(callback);
        }
    }

    @Override
    public void onDestroy() {
        Hub.getInstance().getScanner().stopScanning();
        Hub.getInstance().removeListener(deviceListener);
        Hub.getInstance().shutdown();

        MyoApplication.bus.unregister(this);
        Log.d("TAG", "Service stopped");

        preferences.edit().putBoolean(Constants.CONNECTION_KEY, false).apply();
        preferences.edit().putBoolean(Constants.SYNC_KEY, false).apply();
        preferences.edit().putBoolean(Constants.NOTIFICATION_ACTIVE, false).apply();

        MyoApplication.bus.post(new BusEvent.MyoConnectionStatusEvent(false));
        MyoApplication.bus.post(new BusEvent.MyoSyncStatusEvent(false));

        super.onDestroy();
    }

    private void setToUnlockMode() {
        if (!blockEverything && !lockToggleMode) {
            lockToggleMode = true;

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    lockToggleMode = false;
                }
            }, 500);
        }
    }

    private void toggleLock() {
        if (currentMyo.isUnlocked()) {
            currentMyo.lock();
            waitingForDT = false;
        }
    }

    private void volUp() {
        if (controller == null) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
        } else {
            controller.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
        }
    }

    private void volDown() {
        if (controller == null) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        } else {
            controller.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        }
    }

    private void delayUntilDTChecked() {
        waitingForDT = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (waitingForDT) {
                    playOrPause();
                }
            }
        }, 150);
    }

    @Override
    public void handlePose(Pose pose, Arm arm) {
        MyoApplication.bus.post(new BusEvent.GestureUpdatedEvent(pose, arm));
        if (pose != Pose.DOUBLE_TAP && !currentMyo.isUnlocked()) {
            return;
        }
        switch (pose) {
            case FINGERS_SPREAD:
                delayUntilDTChecked();
//                playOrPause();
//                Toast.makeText(this, pose.name(), Toast.LENGTH_SHORT).show();
                break;
            case FIST:
                fistMade = true;
                break;
            case REST:
                resetFist();
                break;
            case DOUBLE_TAP:
                toggleLock();
                break;
            case WAVE_IN:
                currentMyo.vibrate(Myo.VibrationType.SHORT);
                if (deviceListener.getArm() == Arm.LEFT) {
                    goToNextSong();
                } else {
                    goToPrevSong();
                }
                Toast.makeText(this, pose.name(), Toast.LENGTH_SHORT).show();
                break;
            case WAVE_OUT:
                currentMyo.vibrate(Myo.VibrationType.SHORT);
                if (deviceListener.getArm() == Arm.LEFT) {
                    goToPrevSong();
                } else {
                    goToNextSong();
                }
                Toast.makeText(this, pose.name(), Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    @Override
    public void resetFist() {
        fistMade = false;
        referenceRoll = currentRoll;
    }

    @Override
    public void setCurrentMyo(Myo myo) {
        this.currentMyo = myo;
        boolean isSynced = myo != null;
        MyoApplication.bus.post(new BusEvent.MyoSyncStatusEvent(isSynced));
        preferences.edit().putBoolean(Constants.SYNC_KEY, isSynced).apply();
    }

    @Override
    public void toggleUnlocked(boolean isUnlocked) {
//        unlocked = isUnlocked;

        if (currentMyo == null) {
            return;
        }

        if (!isUnlocked) {
            fistMade = false;
//            currentMyo.lock();
        } else {
            currentMyo.unlock(Myo.UnlockType.HOLD);
        }
    }

    @Override
    public void handleRotationCalc(float pitch, float roll, float yaw) {
        currentRoll = roll;
        currentPitch = pitch;
        currentYaw = yaw;

        if (fistMade) {
            if (!isYawing) {
                isRolling = handleRoll();
            }
        }
    }

    @Override
    public void initReferenceRoll() {
        referenceRoll *= -1;
    }

    @Override
    public void setConnected(boolean isConnected) {
        MyoApplication.bus.post(new BusEvent.MyoConnectionStatusEvent(isConnected));
        preferences.edit().putBoolean(Constants.CONNECTION_KEY, isConnected).apply();
    }

    private boolean handleRoll() {
        double subtractive = currentRoll - referenceRoll;
        if (subtractive > ROLL_THRESHOLD) {
            volUp();
            referenceRoll = currentRoll;
            return true;
        } else if (subtractive < -ROLL_THRESHOLD) {
            volDown();
            referenceRoll = currentRoll;
            return true;
        }
        return fistMade;
    }

    private void handlePitch() {
        // ARM UP IS NEGATIVE, DOWN IS POSITIVE

        double subtractive = currentPitch - referencePitch;
        if (subtractive > PITCH_THRESHOLD) {
//            volUp();
            referencePitch = currentPitch;
//            Log.d("TAG", "PITCH: +");
        } else if (subtractive < -PITCH_THRESHOLD) {
//            volDown();
            referencePitch = currentPitch;
//            Log.d("TAG", "PITCH: -");
        }
    }

    private boolean handleYaw() {
        // INCREASE LEFT, DECREASE RIGHT

        double subtractive = currentYaw - referenceYaw;
        if (subtractive > YAW_THRESHOLD) {
            rewind();
            referenceYaw = currentYaw;
            return true;
        } else if (subtractive < -YAW_THRESHOLD) {
            fastForward();
            referenceYaw = currentYaw;
            return true;
        }
        return fistMade;
    }

    private void goToNextSong() {
        if (controller == null) {
            Intent next = new Intent("com.android.music.musicservicecommand");
            next.putExtra("command", deviceListener.getArm() == Arm.RIGHT ? "next" : "previous");
            sendBroadcast(next);
        } else {
            controller.getTransportControls().skipToNext();
        }
    }

    private void goToPrevSong() {
        if (controller == null) {
            Intent previous = new Intent("com.android.music.musicservicecommand");
            previous.putExtra("command", deviceListener.getArm() == Arm.RIGHT ? "previous" : "next");
            sendBroadcast(previous);
        } else {
            controller.getTransportControls().skipToPrevious();
        }
    }

    private void playOrPause() {
        currentMyo.vibrate(Myo.VibrationType.SHORT);

        if (controller == null) {
            Intent pause = new Intent("com.android.music.musicservicecommand");
            pause.putExtra("command", "togglepause");
            sendBroadcast(pause);
        } else {
            if (playbackState == PlaybackState.STATE_PAUSED) {
                controller.getTransportControls().play();
            } else {
                controller.getTransportControls().pause();
            }
        }
    }

    private void fastForward() {
        if (controller != null) {
            controller.getTransportControls().seekTo(controller.getPlaybackState().getPosition() + 10000);
        }
    }

    private void rewind() {
        if (controller != null) {
            long newPosition = controller.getPlaybackState().getPosition() - 10000;
            controller.getTransportControls().seekTo(newPosition < 0 ? 0 : newPosition);
        }
    }

    @Subscribe
    public void destroyServiceEvent(BusEvent.DestroyServiceEvent event) {
        this.stopSelf();
    }

    @Subscribe
    public void restartMediaController(BusEvent.RestartControllerEvent event) {
        createMediaController();
    }
}
