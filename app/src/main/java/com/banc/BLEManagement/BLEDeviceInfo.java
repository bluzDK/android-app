package com.banc.BLEManagement;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.banc.sparkle_gateway.service.BLEService;

import java.io.IOException;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.utils.Async;

public class BLEDeviceInfo implements Runnable {
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
		ulBuffer = new byte[512];
		ulBufferLength = 0;
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
	public void SetClaimed(boolean claimed) {isClaimed = claimed; }

	//buffers for data
	byte[] dlBuffer, ulBuffer;
	int ulBufferLength;

	//internal flags so the manager can keep track of successful transmissions
	volatile boolean transmissionDone = false;

	//internal var to keep track of last service
	byte lastService = 0x00;

	public boolean Running = false;
	@Override
	public void run() {
		Running = true;
		// TODO Auto-generated method stub
		while (Running) {

			if (particleSocket.isConnected()) {
				try {
					int bytesAvailable = particleSocket.Available();
//					Log.d("BLEService", "isConnected: " + Boolean.toString(sparkSocket.isConnected()) + "  Bytes Available: " + bytesAvailable);
					if (bytesAvailable > 0) {
						dlBuffer = particleSocket.read();
//						Log.d("BLEService", "We read some bytes from SPark Cloud: " + dlBuffer.length);
						byte[] header = {0x01, 0x00};
						BLEManager.send(this, dlBuffer, header);
					}
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void stop()
	{
		Running = false;
	}

	public void disconnect() {
		stop();
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			particleSocket.disconnect();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		mBluetoothGatt.close();

		mBluetoothGatt = null;
		mBluetoothGattService = null;
	}

	//Called when the SparkLE transmits data to us
	public synchronized void processData(byte[] data) {
    	Log.d("BLEService", "Got data of length " + data.length);
		Log.d("BLEService", "Last Service " + Byte.toString(lastService));

		StringBuilder sb = new StringBuilder();
		for (byte b : data) {
			sb.append(String.format("%02X ", b));
		}
//	    Log.d("BLEService", "Processing Data: " + sb.toString());

		if (data[0] == 0x03 && data[1] == 0x04) {
			if (lastService == 0x01) {
				try {
					if (particleSocket.isConnected()) {
						Log.d("BLEService", "Got a full buffer, attempting to send it up");
						byte[] tmpBuffer = new byte[ulBufferLength];
						System.arraycopy(ulBuffer, 0, tmpBuffer, 0, ulBufferLength);
//        			Log.d("BLEService", "About to write this many bytes " + tmpBuffer.length);
						particleSocket.write(tmpBuffer);
//					Log.d("BLEManager", "Received this many bytes from BLE: " + tmpBuffer.length);
					} else {
						try {
							Log.d("SparkLEService", "Not isConnected. Attempting to connect to cloud");
							particleSocket.connect();
							Thread rThread = new Thread(this);
							//rThread.setUncaughtExceptionHandler(new ExceptionHandler());
							rThread.start();
							ulBuffer = new byte[512];
							ulBufferLength = 0;
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (lastService == 0x02) {
				final StringBuilder id = new StringBuilder();
				for (int i = 0; i < ulBufferLength; i++) {
					id.append(String.format("%02x", ulBuffer[i]));
				}
				Log.d("Device ID", id.toString());
				this.cloudId = id.toString();
				Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Object>() {
					@Override
					public Object callApi(ParticleCloud sparkCloud) throws ParticleCloudException, IOException {
						return ParticleCloudSDK.getCloud().getDevice(id.toString()).getName();
					}

					@Override
					public void onSuccess(Object value) {
						String name;
						if (value != null) {
							name = (String) value;
						} else {
							name = "";
						}
						Log.d("Device Name Retreieved", name);
						cloudName = name;
						isClaimed = true;
						BLEService.DeviceInfoChanged();
					}

					@Override
					public void onFailure(ParticleCloudException e) {
						Log.d("Device is not claimed","");
						isClaimed = false;
						BLEService.DeviceInfoChanged();
					}
				});

			}
			ulBuffer = new byte[512];
			ulBufferLength = 0;
		} else {

			if (ulBufferLength == 0) {
				lastService = data[0];
				int headerBytes = 1;
				if (lastService == 0x01) {
					headerBytes = 2;
				}
				//this is the first packaet in the stream. check the header
				System.arraycopy(data, headerBytes, ulBuffer, ulBufferLength, data.length-headerBytes);
				ulBufferLength += (data.length-headerBytes);
			} else {
				System.arraycopy(data, 0, ulBuffer, ulBufferLength, data.length);
				ulBufferLength += (data.length);
			}
		}
		//bleManager.send(new byte[]{0x55});

		//TO DO: Handle BLE messages
	}

	public void PollParticleId() {
		byte[] requestIdBuffer = {0x02, 0x00};
		BLEManager.send(this, requestIdBuffer, null);
	}
}
