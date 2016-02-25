package gateway;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.banc.gateway.R;

import java.io.IOException;
import java.util.List;

import BLEManagement.BLEDeviceInfo;
import BLEManagement.BLEDeviceInfoList;
import BLEManagement.BLEEvent;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;


public class BLESelection extends Activity {

    public final static String DEVICE_MESSAGE = "com.banclabs.gloveapp.DeviceAddress";
    private static final String TAG = "BLESelection" ;

    static private ServiceManager sManager;
    static BLEDeviceInfoList currentDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_list);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.activity_ble_list, null);


        ParticleCloudSDK.init(this);

        this.sManager = new ServiceManager(this, BLEService.class, new HandlerExtension());
        if (!sManager.isRunning()) {
            sManager.start();
        }

        // user is not logged in, so automagically display login thing
        if (!ParticleCloudSDK.getCloud().isLoggedIn()) {
            Intent intent = new Intent(this, ParticleLoginDisplay.class);
            startActivityForResult(intent, 1);
        }
        else {
            Button loginButton = (Button)findViewById(R.id.loginButton);
            loginButton.setText("Logout");
        }

        FloatingActionButton fab = (FloatingActionButton)  view.findViewById(R.id.myFAB);
        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.e(TAG, "FAB pressed");
                Snackbar.make(v, "FAB Clicked", Snackbar.LENGTH_LONG).show();
            }
        });




    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        Button scanButton = (Button)findViewById(R.id.scanButton);

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
        super.onDestroy();  // Always call the superclass method first

        sManager.stop();
        sManager.unbind();
    }

    public void loginButtonPressed(View view) {
        if (ParticleCloudSDK.getCloud().isLoggedIn()) {
            ParticleCloudSDK.getCloud().logOut();
            Button loginButton = (Button)findViewById(R.id.loginButton);
            loginButton.setText("Login");
        } else {
            Intent intent = new Intent(this, ParticleLoginDisplay.class);
            startActivityForResult(intent, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                try {
                    List<ParticleDevice> devices = ParticleCloudSDK.getCloud().getDevices();
                    for (ParticleDevice device : devices) {
                        // Log.d(TAG, device.getName());
                    }
                }
                catch (ParticleCloudException ex) {
                    Log.e("ParticleLoginDisplay", "Received error when getting devices");
                    ex.printStackTrace();
                }
            }
        }
    }

    public void scanButtonPressed(View view) {
        Button scanButton = (Button)findViewById(R.id.scanButton);

        if (scanButton.getText() == "Stop") {
            Message msg = new Message();
            Bundle b = new Bundle();
            b.putInt("info", BLEService.STOP_DISCOVERY);
            msg.setData(b);
            try {
                sManager.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            scanButton.setText("Scan");
        } else {
            Message msg = new Message();
            Bundle b = new Bundle();
            b.putInt("info", BLEService.START_DISCOVERY);
            msg.setData(b);
            try {
                sManager.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            scanButton.setText("Stop");
        }

    }

    Intent intent;
    private void updateTable(BLEDeviceInfoList devices)
    {
        currentDevices = devices;
        ListView list = (ListView)findViewById(R.id.listView1);
        // Getting adapter by passing xml data ArrayList
        DeviceAdapter adapter=new DeviceAdapter(this, devices);
        list.setAdapter(adapter);


        // Click event for single list row
//        list.setOnItemClickListener(new OnItemClickListener() {
//
//			@Override
//			public void onItemClick(AdapterView<?> parent, View view,
//					int position, long id) {
//				android.widget.RelativeLayout item = (android.widget.RelativeLayout)view;
////				TextView addressTextView = (TextView)item.findViewById(R.id.deviceAddress);
//				TextView nameTextView = (TextView)item.findViewById(R.id.deviceName);
////				String address = addressTextView.getText().toString();
//				BLEDeviceInfo devInfo = currentDevices.GetBLEDeviceInfo(position);
//				String address = devInfo.GetMAC();
//				String deviceName = nameTextView.getText().toString();
//				// Log.d("DEBUG", "User selected " + address);
//				Message msg = new Message();
//				Bundle b = new Bundle();
//				b.putInt("info", BLEService.CONNECT);
//				b.putString("address", address);
//				msg.setData(b);
//				try {
//					sManager.send(msg);
//				} catch (RemoteException e) {
//					e.printStackTrace();
//				}
//				sManager.unbind();
//				intent.putExtra(DEVICE_MESSAGE, deviceName);
//				startActivity(intent);
//			}
//
//        });
    }

    private class HandlerExtension extends Handler {

        @Override
        public void handleMessage(Message msg) {
            int type = msg.getData().getInt("BLEEventType", -1);

            if (type != -1)
            {
                //this means it is a BLEEvent from the service
                BLEEvent event = (BLEEvent)msg.obj;
                if (event.BLEEventType == BLEEvent.EVENT_UPDATE || event.BLEEventType == BLEEvent.EVENT_DEVICE_STATE_CHANGE)
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
            //msg.recycle();
        }
    }

    public OnClickListener connectButtonClicked = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = (Integer) v.getTag();
            // Log.d("DEBUG","Clicked Connect Button at Position: " + position);
            BLEDeviceInfo devInfo = currentDevices.GetBLEDeviceInfo(position);
            String address = devInfo.GetMAC();
            // Log.d("DEBUG", "User selected " + address);
            Message msg = new Message();
            Bundle b = new Bundle();
            if (devInfo.State == BLEDeviceInfo.STATE_CONNECTED) {
                b.putInt("info", BLEService.DISCONNECT);
            } else {
                b.putInt("info", BLEService.CONNECT);
            }
            b.putString("address", address);
            msg.setData(b);
            try {
                sManager.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    public OnClickListener claimButtonClicked = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = (Integer) v.getTag();
            // Log.d("DEBUG","Clicked Claim Button at Position: " + position);
            final BLEDeviceInfo devInfo = currentDevices.GetBLEDeviceInfo(position);
            Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Object>() {
                @Override
                public Object callApi(ParticleCloud sparkCloud) throws ParticleCloudException, IOException {
                    ParticleCloudSDK.getCloud().claimDevice(devInfo.GetCloudID());
                    return 1;
                }

                @Override
                public void onSuccess(Object value) {
                    // Log.d("Device Claimed", "");
                    devInfo.SetClaimed(true);
                    Toaster.s(BLESelection.this, "Claimed!");
                    updateTable(currentDevices);
                }

                @Override
                public void onFailure(ParticleCloudException e) {
                    // Log.d("Device Not Claimed", "");
                    Toaster.s(BLESelection.this, "Error Claiming Device!");
                }
            });
        }
    };

}