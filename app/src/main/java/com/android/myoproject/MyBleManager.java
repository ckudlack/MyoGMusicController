package com.android.myoproject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import com.thalmic.myo.internal.ble.Address;
import com.thalmic.myo.internal.ble.BleGatt;
import com.thalmic.myo.internal.ble.BleManager;
//import com.thalmic.myo.internal.ble.JBBluetoothLeController;
import com.thalmic.myo.internal.util.ByteUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MyBleManager implements CustomBleManagerInterface {
    private Context mContext;
    private BluetoothAdapter mAdapter;
    //    private JBBluetoothLeController mController;
    private HashMap<BleManager.BleScanCallback, BluetoothAdapter.LeScanCallback> mCallbacks = new HashMap();

    private Object controller;

    private int counter = 0;

    MyBleManager(Context context) {
        this.mContext = context.getApplicationContext();
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mAdapter = bluetoothManager.getAdapter();
//        this.mController = new JBBluetoothLeController(context);

        Class c = null;
        try {
            c = Class.forName("com.thalmic.myo.internal.ble.JBBluetoothLeController");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            Constructor constructor = c.getDeclaredConstructor(Context.class);
            constructor.setAccessible(true);

            controller = constructor.newInstance(mContext);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    public boolean isBluetoothSupported() {
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le")) {
            return false;
        }
        return this.mAdapter != null;
    }

    public Object getBleThingy() {
        return this.controller;
    }

    @Override
    public boolean startBleScan(BleScanCallback callback) {
        BluetoothAdapter.LeScanCallback leScanCallback = (BluetoothAdapter.LeScanCallback) this.mCallbacks.get(callback);
        if (leScanCallback == null) {
            leScanCallback = createCallback(callback);
            this.mCallbacks.put(callback, leScanCallback);
        }
        return this.mAdapter.startLeScan(leScanCallback);
    }

    @Override
    public void stopBleScan(BleScanCallback callback) {
        BluetoothAdapter.LeScanCallback leScanCallback = (BluetoothAdapter.LeScanCallback) this.mCallbacks.remove(callback);
        this.mAdapter.stopLeScan(leScanCallback);
    }

    public boolean connect(String address, boolean autoConnect) {
        Object retVal = null;

        try {
            Method m = controller.getClass().getDeclaredMethod("connect", String.class, boolean.class);
            m.setAccessible(true);
            retVal = m.invoke(controller, address, autoConnect);
        } catch (NoSuchMethodException e) {
            Log.e("A", e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e("B", e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e("C", e.getMessage());
        }


        return (Boolean) retVal;
    }

    public void disconnect(String address) {
        try {
            Method m = controller.getClass().getDeclaredMethod("disconnect", String.class);
            m.setAccessible(true);
            m.invoke(controller, address);
        } catch (NoSuchMethodException e) {
            Log.e("A", e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e("B", e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e("C", e.getMessage());
        }
    }

    public void dispose() {

        try {
            Method m = controller.getClass().getDeclaredMethod("close");
            m.setAccessible(true);
            m.invoke(controller);
        } catch (NoSuchMethodException e) {
            Log.e("A", e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e("B", e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e("C", e.getMessage());
        }
    }

    private BluetoothAdapter.LeScanCallback createCallback(final BleManager.BleScanCallback callback) {
        return new BluetoothAdapter.LeScanCallback() {
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                Address address = new Address(device.getAddress());

                List<UUID> uuids = MyBleManager.parseServiceUuids(scanRecord);

                UUID serviceUuid = null;
                try {
                    serviceUuid = (UUID) uuids.get(0);
                } catch (IndexOutOfBoundsException e) {
                    Log.e("Caught it!", e.getMessage());
                    return;
                }

                if(counter == 0) {
                    counter++;
                    callback.onBleScan(address, rssi, serviceUuid);
                }
            }
        };
    }

    static List<UUID> parseServiceUuids(byte[] adv_data) {
        List<UUID> uuids = new ArrayList();

        try {

            int offset = 0;
            while (offset < adv_data.length - 2) {
                int len = adv_data[(offset++)];
                if (len == 0) {
                    break;
                }
                int type = adv_data[(offset++)];
                switch (type) {
                    case 2:
                    case 3:
                    case 6:
                    case 7:
                        while (len > 1) {
                            int uuid16 = adv_data[(offset++)];
                            uuid16 += (adv_data[(offset++)] << 8);
                            len -= 2;
                            uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", new Object[]{Integer.valueOf(uuid16)})));

                            continue;
//                        while (len > 15) {
//                            UUID uuid = ByteUtil.getUuidFromBytes(adv_data, offset);
//                            len -= 16;
//                            offset += 16;
//                            uuids.add(uuid);
//                        }
                        }
                }
                offset += len - 1;
            }

        }
        catch (IndexOutOfBoundsException e) {
            Log.e("Whoops", e.getMessage());
        }
        return uuids;
    }

    @Override
    public BleGatt getBleGatt() {
        getBleThingy();
        return (BleGatt) controller;
    }

}
