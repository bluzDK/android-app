package com.banc.sparkle_gateway;

import com.banc.BLEManagement.BLEDeviceInfo;
import com.banc.BLEManagement.BLEDeviceInfoList;
import com.banc.BLEManagement.BLEEvent;
import com.banc.BLEManagement.BLEManager;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.graphics.Typeface;

public class BLEDisplay extends Activity {
	
	private ServiceManager sManager;
	private String address;
	TextView mainTitle;
	String name;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_glove_display);
		
		Intent intent = getIntent();
		name = intent.getStringExtra(BLESelection.DEVICE_MESSAGE);
		
		mainTitle = (TextView)this.findViewById(R.id.textView1);
		mainTitle.setTextColor(Color.GREEN);
		mainTitle.setText("You are connected to: " + name);
		mainTitle.setTypeface(null, Typeface.BOLD);
		
		this.sManager = new ServiceManager(this, BLEService.class, new HandlerExtension());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	@Override
    protected void onDestroy() {
        super.onDestroy();  // Always call the superclass method first
        
        Log.d("GloveDisplay", "Disconnecting from BLE device " + address);
        Message msg = new Message();
		Bundle b = new Bundle();
		b.putInt("info", BLEService.DISCONNECT);
		b.putString("address", address);
		msg.setData(b);
		try {
			sManager.send(msg);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        sManager.unbind();
    }    
	
	private class HandlerExtension extends Handler {
    	
		@Override
		public void handleMessage(Message msg) {
			Log.d("DEBUG", "Received Message in UI");
			int type = msg.getData().getInt("BLEEventType", -1);

			if (type != -1) {
				Log.d("BLEDisplay", "Received BLEEvent in UI");
				//this means it is a BLEEvent from the service
				BLEEvent event = (BLEEvent) msg.obj;
				if (event.BLEEventType == BLEEvent.EVENT_DEVICE_STATE_CHANGE) {
					int newState = event.State;
					if (newState == BLEDeviceInfo.STATE_DISCONNECTED)
					{
						mainTitle.setTextColor(Color.RED);
						mainTitle.setText("You are disconnected from: " + name);
					}
				}
			}
			msg.recycle();
		}		
	}

}
