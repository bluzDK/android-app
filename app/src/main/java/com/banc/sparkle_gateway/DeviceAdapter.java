package com.banc.sparkle_gateway;

import com.banc.BLEManagement.BLEDeviceInfo;
import com.banc.BLEManagement.BLEDeviceInfoList;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DeviceAdapter extends BaseAdapter {
	
	private BLEDeviceInfoList devices;
	private static LayoutInflater inflater=null;
	
	public DeviceAdapter(Activity activity, BLEDeviceInfoList d) {
		devices = d;
		inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {	
		return devices.GetCount();
	}

	@Override
	public Object getItem(int position) {
		return devices.GetBLEDeviceInfo(position);
	}

	@Override
	public long getItemId(int position) {
		return devices.GetBLEDeviceInfo(position).hashCode();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View vi=convertView;
        if(convertView==null)
            vi = inflater.inflate(R.layout.list_row, null);
 
        TextView name = (TextView)vi.findViewById(R.id.deviceName); // title
        TextView address = (TextView)vi.findViewById(R.id.deviceAddress); // artist name
        TextView rssi = (TextView)vi.findViewById(R.id.deviceRssi); // duration
 
        BLEDeviceInfo device = devices.GetBLEDeviceInfo(position);
 
        Log.d("DEBUG", "Adding device with name " + device.GetName());
        // Setting all values in listview
        name.setText(device.GetName());
        address.setText(device.GetMAC());
        rssi.setText(Integer.toString(device.GetRSSI()));
        return vi;
	}

}
