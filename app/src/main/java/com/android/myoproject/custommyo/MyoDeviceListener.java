package com.android.myoproject.custommyo;

import android.content.Context;
import android.widget.Toast;

import com.android.myoproject.callbacks.DeviceCallback;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.XDirection;

public class MyoDeviceListener extends AbstractDeviceListener {

    private Context context;
    private Arm mArm;
    private XDirection mXDirection;
    private DeviceCallback callback;

    public MyoDeviceListener(DeviceCallback callback, Context context) {
        this.callback = callback;
        this.context = context;
    }

    @Override
    public void onArmUnsync(Myo myo, long timestamp) {
        mArm = Arm.UNKNOWN;
        mXDirection = XDirection.UNKNOWN;
        callback.setCurrentMyo(null);
        callback.toggleUnlocked(false);
    }

    @Override
    public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
        callback.setCurrentMyo(myo);
        mArm = arm;
        mXDirection = xDirection;
        myo.vibrate(Myo.VibrationType.SHORT);
        myo.vibrate(Myo.VibrationType.SHORT);

        Toast.makeText(context, "Arm Detected", Toast.LENGTH_SHORT).show();

        callback.toggleUnlocked(false);
        callback.resetFist();
        if (mXDirection == XDirection.TOWARD_ELBOW) {
            callback.initReferenceRoll();
        }
    }

    @Override
    public void onDetach(Myo myo, long timestamp) {
    }

    @Override
    public void onAttach(Myo myo, long timestamp) {
        Toast.makeText(context, "Paired", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnect(Myo myo, long timestamp) {
        Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnect(Myo myo, long timestamp) {
    }

    @Override
    public void onPose(Myo myo, long timestamp, Pose pose) {
        callback.handlePose(pose);
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

        callback.handleRotationCalc(roll);
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

    public Arm getArm() {
        return mArm;
    }
}
