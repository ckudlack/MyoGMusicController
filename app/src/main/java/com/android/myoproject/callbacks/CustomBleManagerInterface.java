package com.android.myoproject.callbacks;

import com.thalmic.myo.internal.ble.BleGatt;
import com.thalmic.myo.internal.ble.BleManager;

public interface CustomBleManagerInterface extends BleManager {

    public abstract boolean isBluetoothSupported();

    public abstract BleGatt getBleGatt();

    public abstract Object getBleThingy();

    public abstract boolean startBleScan(BleScanCallback paramBleScanCallback);

    public abstract void stopBleScan(BleScanCallback paramBleScanCallback);

    public abstract boolean connect(String paramString, boolean paramBoolean);

    public abstract void disconnect(String paramString);

    public abstract void dispose();
}
