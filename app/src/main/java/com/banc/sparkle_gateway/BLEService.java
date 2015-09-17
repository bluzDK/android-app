package com.banc.sparkle_gateway;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Observable;
import java.util.Observer;

import com.banc.BLEManagement.BLEDeviceInfoList;
import com.banc.BLEManagement.BLEEvent;
import com.banc.BLEManagement.BLEManager;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;
import android.view.KeyEvent;

public class BLEService extends AbstractService implements Observer {
	private BLEManager bleManager;
	
    final public static int CONNECT = 2;
    final public static int DISCONNECT = 3;
    final public static int START_DISCOVERY = 4;
    final public static int STOP_DISCOVERY = 5;       
    
    private int MY_DATA_CHECK_CODE = 0;
    
    private boolean askedSMS = false;
    private boolean commandMode = false;
    
    byte[] dlBuffer, ulBuffer;
    int ulBufferLength;
    
    SparkCloudInterface sparkSocket;
    
    AudioManager audio;  
    
    class ReaderThread implements Runnable {

		boolean run = true;
		@Override
		public void run() {
			run = true;
			// TODO Auto-generated method stub
			while (run) {
				try {
					int bytesAvailable = sparkSocket.Available();
					Log.d("BLEService", "Connected: " + Boolean.toString(sparkSocket.Connected()) + "  Bytes Available: " + bytesAvailable);
					if (bytesAvailable > 0) {
						dlBuffer = sparkSocket.Read();
//						Log.d("BLEService", "We read some bytes from SPark Cloud: " + dlBuffer.length);
						bleManager.send(dlBuffer);
						byte[] eosBuffer = {0x03, 0x04};
						bleManager.send(eosBuffer);
					}
					try {
						Thread.sleep(500);
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

		public void stop()
		{
			run = false;
		}
    }
    ReaderThread readerThread;

	@Override
	public void onStartService() {
		bleManager = new BLEManager(this.GetContext());
		bleManager.addObserver(this);
		sparkSocket = new SparkCloudInterface();
	}

	@Override
	public void onStopService() {
		bleManager.deleteObserver(this);
		bleManager.stop();
	}

	@Override
	public void onReceiveMessage(Message msg) {
		// TODO Auto-generated method stub
		Log.d("SparkLEService", "Received message");
		try {
			int info = msg.getData().getInt("info");
			Log.d("SparkLEService", "Received message: " + info);
			switch (info)
			{
//			case GET_INFO:
//				BLEEvent event = new BLEEvent();
//				event.BLEEventType = BLEEvent.EVENT_UPDATE;
//				event.Contents = bleManager.bleDevices;
//				updateUI(event);
//				break;
			case CONNECT:
				String address = msg.getData().getString("address");
				bleManager.connect(address);
				while (!bleManager.isInitialized()) {
					Thread.sleep(100);
				}
				Log.d("BLEService", "We are now connected and initialized!");				
//				Thread.sleep(2000);
//				Log.d("BLEService", "Done Waiting 2 seconds");
//				sparkSocket.Connect();
				//bleManager.send(new byte[]{0x55});
//				readerThread = new ReaderThread();
//				Thread rThread = new Thread(readerThread);
//				//rThread.setUncaughtExceptionHandler(new ExceptionHandler());
//				rThread.start();
//				ulBuffer = new byte[512];
//				ulBufferLength = 0;
				break;
			case DISCONNECT:
				//String address = msg.getData().getString("address");
				Log.d("SparkLEService", "Disconnecting BLE");
				readerThread.stop();
				bleManager.disconnect();
				sparkSocket.Disconnect();
				break;
			case START_DISCOVERY:
				Log.d("SparkLEService", "Starting Discovery");
				bleManager.start();
				break;
			case STOP_DISCOVERY:
				Log.d("SparkLEService", "Stopping Discovery");
				bleManager.stop();
				break;
			
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	@Override
	public void update(Observable observable, Object data) {
		// TODO Auto-generated method stub
//		Log.d("SparkLEServices", "Received event from BLEManager");
		BLEEvent event = (BLEEvent)data;
		handleNewState(event.State);
		switch (event.BLEEventType)
		{
			case BLEEvent.EVENT_UPDATE:
				updateUI(event);
				break;
			case BLEEvent.EVENT_DEVICE_STATE_CHANGE:
				updateUI(event);
				break;
			case BLEEvent.EVENT_RX_DATA:
				processData((byte[]) event.Contents);
				break;
		}
	}

	private void handleNewState(int newState)
	{
		switch (newState)
		{
			case BLEManager.STATE_DISCONNECTED:
				Log.d("BLEService", "Handling BLE disconnection");
				readerThread.stop();
				bleManager.disconnect();
				try {
					sparkSocket.Disconnect();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				break;
		}
	}
	
	private void updateUI(BLEEvent event) {
    	Message msg = Message.obtain(null, 2);
		Bundle b = new Bundle();
		b.putInt("BLEEventType", event.BLEEventType);
		msg.setData(b);		
		msg.obj = event;
		send(msg);
    }
	    
    //Called when the SparkLE transmits data to us
    private synchronized void processData(byte[] data) {
//    	Log.d("BLEService", "Got data: " + data + " of length " + data.length);
    	
    	StringBuilder sb = new StringBuilder();
	    for (byte b : data) {
	        sb.append(String.format("%02X ", b));
	    }
//	    Log.d("BLEService", "Processing Data: " + sb.toString());
        
	    if (data[0] == 0x03 && data[1] == 0x04) {
        	try {
				if (sparkSocket.Connected()) {
					Log.d("BLEService", "Got a full buffer, attempting to send it up");
					byte[] tmpBuffer = new byte[ulBufferLength];
					System.arraycopy(ulBuffer, 0, tmpBuffer, 0, ulBufferLength);
//        			Log.d("BLEService", "About to write this many bytes " + tmpBuffer.length);
					sparkSocket.Write(tmpBuffer);
					Log.d("BLEManager", "Received this many bytes from BLE: " + tmpBuffer.length);
				}
				else {
					try {
						Log.d("SparkLEService", "Not Connected. Attempting to connect to cloud");
						sparkSocket.Connect();
						readerThread = new ReaderThread();
						Thread rThread = new Thread(readerThread);
						//rThread.setUncaughtExceptionHandler(new ExceptionHandler());
						rThread.start();
						ulBuffer = new byte[512];
						ulBufferLength = 0;
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
        	ulBuffer = new byte[512];
        	ulBufferLength = 0;
        } else {
			if (sparkSocket.Connected()) {
//        	Log.d("BLESerivce", "buffer: " + ulBuffer + " has length " + ulBuffer.length + " with current position set to " + ulBufferLength);
				System.arraycopy(data, 0, ulBuffer, ulBufferLength, data.length);
				ulBufferLength += data.length;
			}
        }
        //bleManager.send(new byte[]{0x55});
        
        //TO DO: Handle BLE messages
    }

}
