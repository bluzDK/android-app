package com.banc.sparkle_gateway;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import java.util.UUID;

import com.banc.BLEManagement.BLEDeviceInfoList;
import com.banc.BLEManagement.BLEEvent;

public class BLESelection extends Activity {
	
	public final static String DEVICE_MESSAGE = "com.banclabs.gloveapp.DeviceAddress";
	
	private ServiceManager sManager;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("GloveSelection", "Creating Glove Selection");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ble_list);

		ParticleCloudSDK.init(this);

		this.sManager = new ServiceManager(this, BLEService.class, new HandlerExtension());
		if (!sManager.isRunning()) {
			Log.d("GloveSelection", "Service is not running. Starting!");
			sManager.start();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
    protected void onStart() {
		Log.d("GloveSelection", "Starting Glove Selection");
		//always do this first
		super.onStart();

		Button loginButton = (Button)findViewById(R.id.loginButton);
		Button scanButton = (Button)findViewById(R.id.scanButton);

		if (ParticleCloudSDK.getCloud().isLoggedIn()) {
			loginButton.setText("Logout");
		}
		
		sManager.bind();
		//tell the service to stop discovery
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putInt("info", BLEService.START_DISCOVERY);
		msg.setData(b);
		try {
			sManager.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		intent = new Intent(this, BLEDisplay.class);
	}
	
	@Override
    protected void onStop() {
		Log.d("GloveSelection", "Stopping Glove Selection");
		//always do this first
		super.onStop();
		
		//tell the service to stop discovery
        Message msg = new Message();
		Bundle b = new Bundle();
		b.putInt("info", BLEService.STOP_DISCOVERY);
		msg.setData(b);
		try {
			sManager.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	@Override
    protected void onDestroy() {
		Log.d("GloveSelection", "Destroying Glove Selection");
        super.onDestroy();  // Always call the superclass method first
        
        sManager.stop();
        sManager.unbind();
    }

	public void loginButtonPressed(View view) {
		// Do something in response to button
		Log.d("Clicked", "Clicked");
		if (ParticleCloudSDK.getCloud().isLoggedIn()) {
			ParticleCloudSDK.getCloud().logOut();
			Button loginButton = (Button)findViewById(R.id.loginButton);
			loginButton.setText("Login");
		} else {
			Intent intent = new Intent(this, ParticleLoginDisplay.class);
			startActivity(intent);
		}
	}

	public void scanButtonPressed(View view) {
		// Do something in response to button
		Log.d("Clicked", "Clicked");

	}
	
	Intent intent;
	private void updateTable(BLEDeviceInfoList devices) 
	{
		Log.d("GloveSelection", "Updating UI Table with " + devices.GetCount() + " devices");
		ListView list = (ListView)findViewById(R.id.listView1);
		// Getting adapter by passing xml data ArrayList
        DeviceAdapter adapter=new DeviceAdapter(this, devices);
        list.setAdapter(adapter);
        
 
        // Click event for single list row
        list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				android.widget.RelativeLayout item = (android.widget.RelativeLayout)view;
//				TextView addressTextView = (TextView)item.findViewById(R.id.deviceAddress);
				TextView nameTextView = (TextView)item.findViewById(R.id.deviceName);
//				String address = addressTextView.getText().toString();
				String address = "";
				String deviceName = nameTextView.getText().toString();
				Log.d("DEBUG", "User selected " + address);
				Message msg = new Message();
				Bundle b = new Bundle();
				b.putInt("info", BLEService.CONNECT);
				b.putString("address", address);
				msg.setData(b);
				try {
					sManager.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				sManager.unbind();
				intent.putExtra(DEVICE_MESSAGE, deviceName);
				startActivity(intent);
			}
        
        });
	}
	
	private class HandlerExtension extends Handler {
    	
		@Override
		public void handleMessage(Message msg) {
			int type = msg.getData().getInt("BLEEventType", -1);
			
			if (type != -1)
			{
				//this means it is a BLEEvent from the service
				BLEEvent event = (BLEEvent)msg.obj;
				if (event.BLEEventType == BLEEvent.EVENT_UPDATE)
				{
					BLEDeviceInfoList devices = (BLEDeviceInfoList)event.Contents;
					updateTable(devices);
				}
			} else {
				//otherwise, it is a message from the ServiceManager
				type = msg.getData().getInt("info", -1);
				if (type == ServiceManager.SERVICE_BOUND)
				{
					Message discoverMessage = new Message();
					Bundle b = new Bundle();
					b.putInt("info", BLEService.START_DISCOVERY);
					discoverMessage.setData(b);
					try {
						sManager.send(discoverMessage);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
			msg.recycle();
		}		
	}
}
