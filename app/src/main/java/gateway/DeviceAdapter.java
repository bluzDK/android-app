package gateway;


import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.banc.gateway.R;

import BLEManagement.BLEDeviceInfo;
import BLEManagement.BLEDeviceInfoList;

public class DeviceAdapter extends BaseAdapter {

    private BLEDeviceInfoList devices;
    private static LayoutInflater inflater=null;
    BLESelection parentActivity;

    public DeviceAdapter(Activity activity, BLEDeviceInfoList d) {
        devices = d;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        parentActivity = (BLESelection)activity;
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
//        TextView address = (TextView)vi.findViewById(R.id.deviceAddress); // artist name
        TextView rssi = (TextView)vi.findViewById(R.id.deviceRssi); // duration
        TextView cloudName = (TextView)vi.findViewById(R.id.deviceCloudName); // duration
        TextView cloudId = (TextView)vi.findViewById(R.id.deviceCloudId); // duration
        Button connectButton = (Button)vi.findViewById(R.id.connectButton); // duration
        Button claimButton = (Button)vi.findViewById(R.id.claimButton); // duration

        connectButton.setTag(position);
        connectButton.setOnClickListener(parentActivity.connectButtonClicked);

        claimButton.setTag(position);
        claimButton.setOnClickListener(parentActivity.claimButtonClicked);

        BLEDeviceInfo device = devices.GetBLEDeviceInfo(position);

        connectButton.setText("Connect");
        if (device.State == BLEDeviceInfo.STATE_CONNECTED) {
            connectButton.setText("Disconnect");
        }

        // Log.d("DEBUG", "Adding device with name " + device.GetName());
        // Setting all values in listview
        name.setText(device.GetName());
//        address.setText(device.GetMAC());
        rssi.setText(Integer.toString(device.GetRSSI()));
        cloudId.setText(device.GetCloudID());
        cloudName.setText(device.GetCloudName());
        claimButton.setVisibility(View.INVISIBLE);
        if (device.GetCloudID() != "" && !device.IsClaimed()) {
            claimButton.setVisibility(View.VISIBLE);
        }

        return vi;
    }

}
