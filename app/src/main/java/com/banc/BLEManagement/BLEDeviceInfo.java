package com.banc.BLEManagement;

import android.bluetooth.BluetoothDevice;

public class BLEDeviceInfo {
	private BluetoothDevice bleDevice;
	private int rssi;
	
	public BLEDeviceInfo(BluetoothDevice device, final int rssi)
	{
		this.bleDevice = device;
		this.rssi = rssi;
	}
	
	public void UpdateRSSI(int newRSSI)
	{
		rssi = newRSSI;
	}
	
	public int GetRSSI()
	{
		return rssi;
	}
	public String GetName()
	{
		return bleDevice.getName();
	}
	public String GetMAC()
	{
		return bleDevice.getAddress();
	}
}
