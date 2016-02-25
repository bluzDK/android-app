package com.banc.BLEManagement;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

public class BLEScanner extends Observable implements Runnable, BluetoothAdapter.LeScanCallback {

    private boolean mRunning = false;
    BLEDeviceInfoList newDevices;

    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private ScanCallback mScanCallback;

    public BLEScanner(Context context) {
        mContext = context;
        newDevices = new BLEDeviceInfoList();
    }

    @Override
    public void run() {
        mRunning = true;

        Log.d("BLEScanner", "Running Scan!");
        newDevices.clearNonConnected();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d("DEBUG", "BLE Scan Started");

            BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();

            mScanCallback = new ScanCallback() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    BluetoothDevice device = result.getDevice();
                    onLeScan(device, result.getRssi(), null);
                }

                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                    for (ScanResult result : results) {
                        BluetoothDevice device = result.getDevice();
                        onLeScan(device, result.getRssi(), null);
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                }
            };

            /*ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
            List<ScanFilter> filters = new ArrayList<>();*/

            scanner.startScan(/*filters, settings, */mScanCallback);
        } else {
            boolean result = mBluetoothAdapter.startLeScan(this);
            Log.d("DEBUG", "BLE Scan Started " + result);
        }
        while (mRunning) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        stop();
    }

    public void stop() {
        mRunning = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
            scanner.stopScan(mScanCallback);
        } else {
            mBluetoothAdapter.stopLeScan(this);
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        Log.d("BLEScanner", "Found device " + device.getName());
        BLEDeviceInfo bleDevice = new BLEDeviceInfo(device, rssi);
        newDevices.InsertOrUpdate(bleDevice);
        setChanged();
        notifyObservers(newDevices);
    }

}
