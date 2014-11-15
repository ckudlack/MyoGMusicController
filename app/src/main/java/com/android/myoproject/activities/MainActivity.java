package com.android.myoproject.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.myoproject.BusEvent;
import com.android.myoproject.application.MyoApplication;
import com.android.myoproject.R;
import com.squareup.otto.Subscribe;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.XDirection;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;

import api.api.impl.InvalidCredentialsException;
import api.api.model.Playlists;
import api.api.model.QueryResponse;
import api.api.model.QueryResults;
import api.api.model.Song;


public class MainActivity extends Activity {

    // This code will be returned in onActivityResult() when the enable Bluetooth activity exits.
    private static final int REQUEST_ENABLE_BT = 1;
    public static final String PLAYSTATE_CHANGED = "com.android.music.playstatechanged";

    private Playlists playlists;
    private MediaPlayer player;

    boolean unlocked = false;

    private AudioManager audioManager;

    private static boolean TEST_MODE = false;

    private boolean isPlaying = false;

    private Arm mArm = Arm.UNKNOWN;
    private XDirection mXDirection = XDirection.UNKNOWN;

    private static float ROLL_THRESHOLD = 1.3f;
    private static float UNLOCK_THRESHOLD = 15;
    private static int BLOCK_TIME = 2000;

    private boolean blockEverything = false;
    private double referenceRoll = 0;
    private double currentRoll = 0;
    private boolean fistMade = false;
    private boolean lockToggleMode = false;

    private Myo currentMyo;

    private DeviceListener listener = new AbstractDeviceListener() {

        @Override
        public void onPair(Myo myo, long timestamp) {
            Toast.makeText(MainActivity.this, "Paired", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(Myo myo, long timestamp) {
            Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDisconnect(Myo myo, long timestamp) {
        }

        @Override
        public void onArmRecognized(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
            currentMyo = myo;
            mArm = arm;
            mXDirection = xDirection;
            myo.vibrate(Myo.VibrationType.SHORT);
            myo.vibrate(Myo.VibrationType.SHORT);

            Toast.makeText(MainActivity.this, "Arm Detected", Toast.LENGTH_SHORT).show();

            unlocked = false;
            resetFist();

            if (mXDirection == XDirection.TOWARD_ELBOW) {
                referenceRoll *= -1;
            }
        }

        @Override
        public void onArmLost(Myo myo, long timestamp) {
            mArm = Arm.UNKNOWN;
            mXDirection = XDirection.UNKNOWN;
            currentMyo = null;
            unlocked = false;
        }

        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            handlePose(pose);
        }

        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
            // Calculate Euler angles (roll, pitch, and yaw) from the quaternion.
            float roll = (float) Math.toDegrees(Quaternion.roll(rotation));
            float pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
            float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation)); // Yaw is arm twist?
            // Adjust roll and pitch for the orientation of the Myo on the arm.
            if (mXDirection == XDirection.TOWARD_ELBOW) {
                roll *= -1;
            }

            currentRoll = roll;

//            Log.d("d", "Roll: " + roll);

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
        public void onAccelerometerData(Myo myo, long timestamp, Vector3 accel) {
            double x = accel.x();
            double y = accel.y();
            double z = accel.z();
        }

        @Override
        public void onGyroscopeData(Myo myo, long timestamp, Vector3 gyro) {
            double x = gyro.x();
            double y = gyro.y();
            double z = gyro.z();
        }

        @Override
        public void onRssi(Myo myo, long timestamp, int rssi) {
            super.onRssi(myo, timestamp, rssi);
        }
    };
    private File[] files;
    private int fileIndex = 0;

    private Button playPause;
    private Button prev;
    private Button next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (TEST_MODE) {
            MyoApplication.bus.register(this);

            Hub hub = Hub.getInstance();
            if (!hub.init(this, getPackageName())) {
                Log.e("TAG", "Could not init Hub");
                finish();
                return;
            }

            hub.addListener(listener);

            Button b = (Button) findViewById(R.id.scan);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onScanActionSelected();
                }
            });

//        hub.pairWithAnyMyo();

            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            playPause = (Button) findViewById(R.id.play_pause);
            next = (Button) findViewById(R.id.next);
            prev = (Button) findViewById(R.id.prev);


            playPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playOrPause();
                }
            });
            prev.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goToPrevSong();
                }
            });
            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goToNextSong();
                }
            });
        } else {
            startActivity(new Intent(this, StartServiceActivity.class));
            finish();
        }
    }

    private void onScanActionSelected() {


/*        try {
            Field f = Hub.getInstance().getClass().getDeclaredField("mScanner");
            f.setAccessible(true);

            Field scannedListener = f.getClass().getDeclaredField("mMyoScannedListener");
            scannedListener.setAccessible(true);

            Field clickedListener = f.getClass().getDeclaredField("mMyoClickedListener");
            clickedListener.setAccessible(true);



//            f.set(Hub.getInstance(), new MyScanner());
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }*/


/*        try {
            Method m = controller.getClass().getDeclaredMethod("connect", String.class, boolean.class);
            m.setAccessible(true);
            retVal = m.invoke(controller, address, autoConnect);
        } catch (NoSuchMethodException e) {
            Log.e("A", e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e("B", e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e("C", e.getMessage());
        }*/

//        Hub.getInstance().getScanner().setBleManager(new MyBleManager(this));

//        Hub.getInstance().getScanner();

//        Intent intent = new Intent(this, ScanActivity.class);
//        startActivity(intent);

        if (Hub.getInstance().getConnectedDevices().size() == 0) {
            Hub.getInstance().pairWithAnyMyo();
        }
    }

    private void handlePose(Pose pose) {
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

    private void resetFist() {
        fistMade = false;
        referenceRoll = currentRoll;
    }

    private void runScript() {
        try {
            Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "/mnt/sdcard/touch.sh",});
        } catch (IOException e) {
            Log.e("TAG", e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If Bluetooth is not enabled, request to turn it on.
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onDestroy() {
        if (TEST_MODE) {
            // We don't want any callbacks when the Activity is gone, so unregister the listener.
            Hub.getInstance().removeListener(listener);
            if (isFinishing()) {
                // The Activity is finishing, so shutdown the Hub. This will disconnect from the Myo.
                Hub.getInstance().shutdown();
            }

            MyoApplication.bus.unregister(this);
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth, so exit.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void getMp3AndPlay() {
        File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        files = downloadsFolder.listFiles();

        Toast.makeText(this, "Files: " + files.length, Toast.LENGTH_LONG).show();

        player = new MediaPlayer();

        try {
            player.setDataSource(this, Uri.parse(files[fileIndex].getPath()));
        } catch (IOException e) {
            Log.e("SET DATA", e.getMessage());
        }

        try {
            player.prepare();
        } catch (IOException e) {
            Log.e("PREPARE", e.getMessage());
        }

        player.start();

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                goToNextSong();

            }
        });
    }

    private void goToNextSong() {
        Intent next = new Intent("com.android.music.musicservicecommand");
        next.putExtra("command", mArm == Arm.RIGHT ? "next" : "previous");
        sendBroadcast(next);
    }

    private void goToPrevSong() {
        Intent previous = new Intent("com.android.music.musicservicecommand");
        previous.putExtra("command", mArm == Arm.RIGHT ? "previous" : "next");
        sendBroadcast(previous);
    }

    private void playOrPause() {
        Intent pause = new Intent("com.android.music.musicservicecommand");
        pause.putExtra("command", isPlaying ? "pause" : "play");
        sendBroadcast(pause);
    }


    private void volUp() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }

    private void volDown() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
    }

    @Subscribe
    public void playbackUpdated(BusEvent.PlaybackUpdatedEvent event) {
        this.isPlaying = event.isPlaying();
    }

    //================================================================================
    // Google Play Music
    //================================================================================

    QueryResponse response;

    private void getGoogleMusic() {
        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                try {
                    MyoApplication.getAPI().login("chris.kudlack@gmail.com", "");
                    response = MyoApplication.getAPI().search("the");
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                } catch (InvalidCredentialsException e) {
                    e.printStackTrace();
                }

                /*String size = String.valueOf(playlists.getPlaylists().size());
                Log.d("PLAYLISTS", size);
                Toast.makeText(MainActivity.this, "Playlist size: " + size, Toast.LENGTH_LONG).show();*/

                QueryResults results = response.getResults();
                Collection<Song> songs = results.getSongs();


                return null;
            }
        };

        task.execute();
    }
}
