package com.banc.BLEManagement;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;

import com.banc.sparkle_gateway.ParticleSocket;

public class BLEDeviceInfo {
	final public static int STATE_BLUETOOTH_OFF = 1;
	final public static int STATE_DISCONNECTED = 2;
	final public static int STATE_CONNECTING = 3;
	final public static int STATE_CONNECTED = 4;

	public int State;
	public ParticleSocket particleSocket;
	public BluetoothGatt mBluetoothGatt;
	public BluetoothGattService mBluetoothGattService;

	private BluetoothDevice bleDevice;
	private int rssi;
	private String cloudName;
	private String cloudId;
	private Boolean isClaimed;

	
	public BLEDeviceInfo(BluetoothDevice device, final int rssi)
	{
		this.bleDevice = device;
		this.rssi = rssi;
		cloudName = "";
		cloudId = "";
		isClaimed = false;
		particleSocket = new ParticleSocket();
	}
	
	public void UpdateRSSI(int newRSSI)
	{
		rssi = newRSSI;
	}
	
	public int GetRSSI() { return rssi; }
	public String GetName() { return bleDevice.getName(); }
	public String GetMAC() { return bleDevice.getAddress(); }
	public String GetCloudID() { return cloudId; }
	public String GetCloudName() { return cloudName; }
	public boolean IsClaimed() {return isClaimed; }



}
