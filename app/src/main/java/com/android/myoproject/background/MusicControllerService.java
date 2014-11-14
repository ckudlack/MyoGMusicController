package com.android.myoproject.background;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.myoproject.BusEvent;
import com.android.myoproject.R;
import com.android.myoproject.custommyo.MyoDeviceListener;
import com.android.myoproject.application.MyoApplication;
import com.android.myoproject.callbacks.DeviceCallback;
import com.squareup.otto.Subscribe;
import com.thalmic.myo.Arm;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;

public class MusicControllerService extends Service implements DeviceCallback {

    private static float ROLL_THRESHOLD = 1.3f;
    private static float UNLOCK_THRESHOLD = 15;
    private static int BLOCK_TIME = 2000;
    private static final int NOTIFICATION_ID = 50990;

    private boolean blockEverything = false;
    private double referenceRoll = 0;
    private double currentRoll = 0;
    private boolean fistMade = false;
    private boolean lockToggleMode = false;
    private boolean unlocked = false;

    private Myo currentMyo;
    private boolean isPlaying = false;


    private AudioManager audioManager;
    private MyoDeviceListener deviceListener;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        MyoApplication.bus.register(this);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        deviceListener = new MyoDeviceListener(this, this);

        Hub hub = Hub.getInstance();
        if (!hub.init(this, getPackageName())) {
            Log.e("TAG", "Could not init Hub");
            this.stopSelf();
            return;
        }

        hub.addListener(deviceListener);
//        hub.pairWithAnyMyo();

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Myo")
                        .setOngoing(true)
                        .setSmallIcon(R.drawable.ic_launcher)
                                //.setContent(setupRemoteViews(nextPendingIntent))
                        .setContentText("Running");
        startForeground(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onDestroy() {
        Hub.getInstance().removeListener(deviceListener);
        // The Activity is finishing, so shutdown the Hub. This will disconnect from the Myo.
        Hub.getInstance().shutdown();

        MyoApplication.bus.unregister(this);
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
        // Unlocked when the user does the THUMB_TO_PINKY gesture and a clockwise hand rotation within 500ms

        lockToggleMode = false;
        unlocked = !unlocked;

        blockEverything = true;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                blockEverything = false;
            }
        }, BLOCK_TIME);

        currentMyo.vibrate(Myo.VibrationType.SHORT);
        currentMyo.vibrate(Myo.VibrationType.SHORT);

        Toast.makeText(this, unlocked ? "Unlocked" : "Locked", Toast.LENGTH_SHORT).show();
    }

    private void volUp() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }

    private void volDown() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
    }

    @Override
    public void handlePose(Pose pose) {
        switch (pose) {
            case FINGERS_SPREAD:
                if (unlocked) {
                    currentMyo.vibrate(Myo.VibrationType.SHORT);
                    playOrPause();
                    Toast.makeText(this, pose.name(), Toast.LENGTH_SHORT).show();
                }
                break;
            case FIST:
                if (unlocked && !fistMade) {
                    fistMade = true;
                }
                break;
            case REST:
                resetFist();
                break;
            case THUMB_TO_PINKY:
                setToUnlockMode();
                break;
            case WAVE_IN:
                if (unlocked) {
                    currentMyo.vibrate(Myo.VibrationType.SHORT);
                    goToPrevSong();
                    Toast.makeText(this, pose.name(), Toast.LENGTH_SHORT).show();
                }
                break;
            case WAVE_OUT:
                if (unlocked) {
                    currentMyo.vibrate(Myo.VibrationType.SHORT);
                    goToNextSong();
                    Toast.makeText(this, pose.name(), Toast.LENGTH_SHORT).show();
                }
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
    }

    @Override
    public void toggleUnlocked(boolean isUnlocked) {
        unlocked = isUnlocked;
    }

    @Override
    public void handleRotationCalc(float roll) {
        currentRoll = roll;

        if (fistMade) {
            double subtractive = currentRoll - referenceRoll;
            if (subtractive > ROLL_THRESHOLD) {
//                    Log.d("h", "+");
                volUp();
                referenceRoll = currentRoll;
            } else if (subtractive < -ROLL_THRESHOLD) {
//                    Log.d("h", "-");
                volDown();
                referenceRoll = currentRoll;
            }
        }

        if (lockToggleMode) {
            double subtractive = currentRoll - referenceRoll;

            if (subtractive > UNLOCK_THRESHOLD) {
                toggleLock();
                referenceRoll = currentRoll;
            }
        }
    }

    @Override
    public void initReferenceRoll() {
        referenceRoll *= -1;
    }

    private void goToNextSong() {
        Intent next = new Intent("com.android.music.musicservicecommand");
        next.putExtra("command", deviceListener.getArm() == Arm.RIGHT ? "next" : "previous");
        sendBroadcast(next);
    }

    private void goToPrevSong() {
        Intent previous = new Intent("com.android.music.musicservicecommand");
        previous.putExtra("command", deviceListener.getArm() == Arm.RIGHT ? "previous" : "next");
        sendBroadcast(previous);
    }

    private void playOrPause() {
        Intent pause = new Intent("com.android.music.musicservicecommand");
        pause.putExtra("command", isPlaying ? "pause" : "play");
        sendBroadcast(pause);
    }

    @Subscribe
    public void playbackUpdated(BusEvent.PlaybackUpdatedEvent event) {
        this.isPlaying = event.isPlaying();
    }

}
