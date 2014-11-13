package com.android.myoproject;

import com.thalmic.myo.internal.ble.Address;
import com.thalmic.myo.internal.ble.BleGatt;
import com.thalmic.myo.internal.ble.BleManager;

import java.util.UUID;

/**
 * Created by Chris on 11/11/14.
 */
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
