package com.android.myoproject.custommyo;

import android.os.Handler;
import android.util.Log;

import com.thalmic.myo.Myo;
import com.thalmic.myo.internal.ble.Address;
import com.thalmic.myo.internal.ble.BleManager;
import com.thalmic.myo.internal.util.ByteUtil;
//import com.thalmic.myo.scanner.MyoDeviceListAdapter;
import com.thalmic.myo.scanner.Scanner;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;


public class MyScanner extends Scanner {
    private static final String TAG = "Scanner";
    private static UUID sAdvertisedUuid;
    private static final long SCAN_PERIOD = 5000L;
    private BleManager mBleManager;
    private Handler mHandler;
    private boolean mScanning;

    static
    {
        try
        {
            System.loadLibrary("gesture-classifier");
        }
        catch (UnsatisfiedLinkError e)
        {
            if (isDalvikVm()) {
                throw e;
            }
        }
    }

    private final Runnable mStopRunnable = new Runnable()
    {
        public void run()
        {
            MyScanner.this.stopScanning();
        }
    };
    private long mRestartInterval;
    private final Runnable mRestartRunnable = new Runnable()
    {
        public void run()
        {
            MyScanner.this.restartScanning();
            if (MyScanner.this.mRestartInterval > 0L) {
                MyScanner.this.mHandler.postDelayed(MyScanner.this.mRestartRunnable, MyScanner.this.mRestartInterval);
            }
        }
    };
    private ArrayList<OnScanningStartedListener> mScanningStartedListeners = new ArrayList();
    private OnMyoScannedListener mMyoScannedListener;
    private OnMyoClickedListener mMyoClickedListener;
//    private MyoDeviceListAdapter mListAdapter = new MyoDeviceListAdapter();
    private BleManager.BleScanCallback mBleScanCallback = new ScanCallback();

    private Object listAdapter;

    public MyScanner(BleManager bleManager, OnMyoScannedListener scannedListener, OnMyoClickedListener clickedListener)
    {
        super(bleManager, scannedListener, clickedListener);
        this.mBleManager = bleManager;
        this.mMyoScannedListener = scannedListener;
        this.mMyoClickedListener = clickedListener;
        this.mHandler = new Handler();

        Class c = null;
        try {
            c = Class.forName("com.thalmic.myo.scanner.MyoDeviceListAdapter");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            Constructor constructor = c.getDeclaredConstructor();
            constructor.setAccessible(true);

            listAdapter = constructor.newInstance();
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

    public void setBleManager(BleManager bleManager)
    {
        this.mBleManager = bleManager;
    }

    public void startScanning()
    {
        startScanning(5000L);
    }

    public void startScanning(long scanPeriod)
    {
        startScanning(scanPeriod, 0L);
    }

    public void startScanning(long scanPeriod, long restartInterval)
    {
        if (this.mScanning)
        {
            Log.w("Scanner", "Scan is already in progress. Ignoring call to startScanning.");
            return;
        }
        this.mHandler.removeCallbacks(this.mStopRunnable);
        if (scanPeriod > 0L) {
            this.mHandler.postDelayed(this.mStopRunnable, scanPeriod);
        }
        this.mHandler.removeCallbacks(this.mRestartRunnable);
        if (restartInterval > 0L) {
            this.mHandler.postDelayed(this.mRestartRunnable, restartInterval);
        }
        this.mRestartInterval = restartInterval;

        boolean started = this.mBleManager.startBleScan(this.mBleScanCallback);
        if (started)
        {
            this.mScanning = true;

//            this.mListAdapter.clear();

            try {
                Method m = listAdapter.getClass().getDeclaredMethod("clear");
                m.setAccessible(true);
                m.invoke(listAdapter);
            } catch (NoSuchMethodException e) {
                Log.e("A", e.getMessage());
            } catch (InvocationTargetException e) {
                Log.e("B", e.getMessage());
            } catch (IllegalAccessException e) {
                Log.e("C", e.getMessage());
            }

            for (OnScanningStartedListener listener : this.mScanningStartedListeners) {
                listener.onScanningStarted();
            }
        }
    }

    public void stopScanning()
    {
        this.mScanning = false;
        this.mHandler.removeCallbacks(this.mStopRunnable);
        this.mHandler.removeCallbacks(this.mRestartRunnable);
        this.mBleManager.stopBleScan(this.mBleScanCallback);
        for (OnScanningStartedListener listener : this.mScanningStartedListeners) {
            listener.onScanningStopped();
        }
    }

    private void restartScanning()
    {
        this.mBleManager.stopBleScan(this.mBleScanCallback);
        boolean started = this.mBleManager.startBleScan(this.mBleScanCallback);
        if (!started) {
            for (OnScanningStartedListener listener : this.mScanningStartedListeners) {
                listener.onScanningStopped();
            }
        }
    }

    public boolean isScanning()
    {
        return this.mScanning;
    }

    public void addOnScanningStartedListener(OnScanningStartedListener listener)
    {
        this.mScanningStartedListeners.add(listener);
        if (this.mScanning) {
            listener.onScanningStarted();
        }
    }

    public void removeOnScanningStartedListener(OnScanningStartedListener listener)
    {
        this.mScanningStartedListeners.remove(listener);
    }

    public OnMyoClickedListener getOnMyoClickedListener()
    {
        return this.mMyoClickedListener;
    }

    public ScanListAdapter getScanListAdapter()
    {
        return (ScanListAdapter) getListAdapter();
    }

/*    MyoDeviceListAdapter getAdapter()
    {
        return this.mListAdapter;
    }*/

    Object getListAdapter() {
        return this.listAdapter;
    }

    public static boolean isMyo(UUID serviceUuid)
    {
        if (sAdvertisedUuid == null)
        {
            byte[] uuidBytes = getServiceInfoUuidBytes();
            sAdvertisedUuid = ByteUtil.getUuidFromBytes(uuidBytes, 0);
        }
        return sAdvertisedUuid.equals(serviceUuid);
    }

    private static boolean isDalvikVm()
    {
        return "Dalvik".equals(System.getProperty("java.vm.name"));
    }

    private static native byte[] getServiceInfoUuidBytes();

    public static abstract interface OnScanningStartedListener
    {
        public abstract void onScanningStarted();

        public abstract void onScanningStopped();
    }

    private class ScanCallback
            implements BleManager.BleScanCallback
    {
        private ScanCallback() {}

        public void onBleScan(final Address address, final int rssi, final UUID serviceUuid)
        {
            MyScanner.this.mHandler.post(new Runnable()
            {
                public void run()
                {
                    if (MyScanner.isMyo(serviceUuid))
                    {
                        Myo myo = MyScanner.this.mMyoScannedListener.onMyoScanned(address, rssi);
//                        MyScanner.this.mListAdapter.addDevice(myo, rssi);

                        try {
                            Method m = listAdapter.getClass().getDeclaredMethod("addDevice", Myo.class, int.class);
                            m.setAccessible(true);
                            m.invoke(listAdapter, myo, rssi);
                        } catch (NoSuchMethodException e) {
                            Log.e("A", e.getMessage());
                        } catch (InvocationTargetException e) {
                            Log.e("B", e.getMessage());
                        } catch (IllegalAccessException e) {
                            Log.e("C", e.getMessage());
                        }
                    }
                }
            });
        }
    }
}
