package com.android.myoproject.callbacks;

import com.thalmic.myo.Arm;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;

public interface DeviceCallback {
    void handlePose(Pose pose, Arm arm);
    void resetFist();
    void setCurrentMyo(Myo myo);
    void toggleUnlocked(boolean isUnlocked);
    void handleRotationCalc(float pitch, float roll, float yaw);
    void initReferenceRoll();
}
