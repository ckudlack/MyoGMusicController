package com.android.myoproject.callbacks;

import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;

public interface DeviceCallback {
    void handlePose(Pose pose);
    void resetFist();
    void setCurrentMyo(Myo myo);
    void toggleUnlocked(boolean isUnlocked);
    void handleRotationCalc(float roll);
    void initReferenceRoll();
}