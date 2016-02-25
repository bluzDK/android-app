package gateway;

import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.Observable;
import java.util.Observer;

import BLEManagement.BLEDeviceInfo;
import BLEManagement.BLEEvent;
import BLEManagement.BLEManager;

public class BLEService extends AbstractService implements Observer {
    final public static int CONNECT = 2;
    final public static int DISCONNECT = 3;
    final public static int START_DISCOVERY = 4;
    final public static int STOP_DISCOVERY = 5;
    AudioManager audio;
    private BLEManager bleManager;
    private int MY_DATA_CHECK_CODE = 0;
    private boolean askedSMS = false;
    private boolean commandMode = false;

    static public void DeviceInfoChanged() {
        BLEEvent event = new BLEEvent();
        event.Contents = BLEManager.GetList();
        event.BLEEventType = BLEEvent.EVENT_DEVICE_STATE_CHANGE;
        event.State = 0;
        event.DeviceInfo = null;
        Message msg = Message.obtain(null, 2);
        Bundle b = new Bundle();
        b.putInt("BLEEventType", event.BLEEventType);
        msg.setData(b);
        msg.obj = event;
        send(msg);
    }

    @Override
    public void onStartService() {
        bleManager = new BLEManager(this.GetContext());
        bleManager.addObserver(this);
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
        String address;
        try {
            int info = msg.getData().getInt("info");
            Log.d("SparkLEService", "Received message: " + info);
            switch (info) {
//			case GET_INFO:
//				BLEEvent event = new BLEEvent();
//				event.BLEEventType = BLEEvent.EVENT_UPDATE;
//				event.Contents = bleManager.bleDevices;
//				updateUI(event);
//				break;
                case CONNECT:
                    address = msg.getData().getString("address");
                    bleManager.connect(address);
                    while (!bleManager.isInitialized(address)) {
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
                    address = msg.getData().getString("address");
                    BLEDeviceInfo devInfo = bleManager.GetBLEDeviceInfoByAddress(address);
                    bleManager.disconnect(address);
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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        // TODO Auto-generated method stub
//		Log.d("SparkLEServices", "Received event from BLEManager");
        BLEEvent event = (BLEEvent) data;
        if (event.DeviceInfo == null) {
            Log.d("WTF!!!", "devInfo is null!");
        }
        switch (event.BLEEventType) {
            case BLEEvent.EVENT_UPDATE:
                updateUI(event);
                break;
            case BLEEvent.EVENT_DEVICE_STATE_CHANGE:
                handleNewState(event.DeviceInfo, event.State);
                updateUI(event);
                break;
            case BLEEvent.EVENT_RX_DATA:
                Log.d("BLEService", "Calling devInfo processData");
                event.DeviceInfo.processData((byte[]) event.Contents);
                break;
        }
    }

    private void handleNewState(final BLEDeviceInfo devInfo, int newState) {
        switch (newState) {
            case BLEDeviceInfo.STATE_DISCONNECTED:
                Log.d("BLEService", "Handling BLE disconnection");
                devInfo.disconnect();
                break;
            case BLEDeviceInfo.STATE_CONNECTED:
                Log.d("BLEService", "Starting timer for Get ID!!");
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {

                    public void run() {
                        Log.d("BLEService", "Running Get ID!!");
                        devInfo.PollParticleId();
                    }
                }, 22000);
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

}
