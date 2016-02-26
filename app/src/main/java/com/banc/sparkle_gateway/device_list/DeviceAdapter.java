package com.banc.sparkle_gateway.device_list;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.banc.BLEManagement.BLEDeviceInfo;
import com.banc.BLEManagement.BLEDeviceInfoList;
import com.banc.sparkle_gateway.R;

public class DeviceAdapter extends BaseAdapter {

    private BLEDeviceInfoList mDevices;
    private BLESelectionActivity mParentActivity;

    public DeviceAdapter(Activity activity) {
        mParentActivity = (BLESelectionActivity) activity;
    }

    public void updateDevices(BLEDeviceInfoList devices) {
        this.mDevices = devices;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row, parent, false);
        }

        TextView name = (TextView) convertView.findViewById(R.id.deviceName);
        TextView rssi = (TextView) convertView.findViewById(R.id.deviceRssi);
        TextView cloudName = (TextView) convertView.findViewById(R.id.deviceCloudName);
        TextView cloudId = (TextView) convertView.findViewById(R.id.deviceCloudId);
        Button connectButton = (Button) convertView.findViewById(R.id.connectButton);
        Button claimButton = (Button) convertView.findViewById(R.id.claimButton);

        connectButton.setTag(position);
        connectButton.setOnClickListener(mParentActivity.connectButtonClicked);

        claimButton.setTag(position);
        claimButton.setOnClickListener(mParentActivity.claimButtonClicked);

        BLEDeviceInfo device = mDevices.GetBLEDeviceInfo(position);

        connectButton.setText(R.string.connect);
        if (device.State == BLEDeviceInfo.STATE_CONNECTED) {
            connectButton.setText(R.string.disconnect);
        } else if (device.State == BLEDeviceInfo.STATE_CONNECTING) {
            connectButton.setText(R.string.connecting);
        }

        Log.d("DEBUG", "Adding device with name " + device.GetName());
        // Setting all values in listview
        name.setText(device.GetName());
        String rssiStr = device.GetRSSI() + "";
        rssi.setText(rssiStr);
        cloudId.setText(device.GetCloudID());
        cloudName.setText(device.GetCloudName());

        if (!TextUtils.isEmpty(device.GetCloudID()) && !device.IsClaimed()) {
            claimButton.setVisibility(View.VISIBLE);
        } else {
            claimButton.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }

    @Override
    public int getCount() {
        return mDevices == null ? 0 : mDevices.GetCount();
    }

    @Override
    public Object getItem(int position) {
        return mDevices.GetBLEDeviceInfo(position);
    }

    @Override
    public long getItemId(int position) {
        return mDevices.GetBLEDeviceInfo(position).hashCode();
    }

}
