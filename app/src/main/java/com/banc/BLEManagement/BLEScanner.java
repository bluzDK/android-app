package com.banc.BLEManagement;

import java.util.Observable;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class BLEScanner extends Observable implements Runnable, BluetoothAdapter.LeScanCallback {
	
	private boolean runScanner = false;
	BLEDeviceInfoList newDevices;
	
	public BLEScanner()
	{
		
	}
	
	@Override
	public void run() {
		runScanner = true;
		BLEDeviceInfoList devices = new BLEDeviceInfoList();
		
		while (runScanner)
		{
			Log.d("BLEScanner", "Running Scan!");
			newDevices = new BLEDeviceInfoList();
			BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			bluetoothAdapter.startLeScan(
	                //new UUID[]{ BLEManager.UUID_SERVICE },
	                this);
	        Log.d("DEBUG", "BLE Scan Started");
	        
	        try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        //HACK FOR NOW!!! NOT WHAT I WANT TO DO!!!!!
	        //This is always updating the UI, which is very inefficient
	        //Instead, we should only search for devices that are lost/added
	        bluetoothAdapter.stopLeScan(this);
		}   
	}
	
	public void stop()
	{
		runScanner = false;
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
