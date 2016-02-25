package BLEManagement;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

public class BLEManager extends Observable implements Observer {

    private final static String ACTION_CONNECTED =
            "com.banc.ACTION_CONNECTED";
    private final static String ACTION_DISCONNECTED =
            "com.banc.ACTION_DISCONNECTED";
    private final static String ACTION_DATA_AVAILABLE =
            "com.banc.ACTION_DATA_AVAILABLE";
    private final static String EXTRA_DATA =
            "com.banc.EXTRA_DATA";
    //    public final static UUID UUID_SERVICE = BluetoothHelper.sixteenBitUuid(0x2220);
    private final static UUID UUID_SERVICE = BluetoothHelper.sixteenBitUuid(0x0223);
    private final static UUID UUID_RECEIVE = BluetoothHelper.sixteenBitUuid(0x0224);
    private final static UUID UUID_SEND = BluetoothHelper.sixteenBitUuid(0x0225);
    //public final static UUID UUID_DISCONNECT = BluetoothHelper.sixteenBitUuid(0x2223);
    private final static UUID UUID_CLIENT_CONFIGURATION = BluetoothHelper.sixteenBitUuidOld();
    private static final String TAG = "SparkLE BLE Manager";
    private static BLEScanner scanner;
    private final BLEDeviceInfoList bleDevices;
    private final Context context;
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BLEDeviceInfo devInfo = GetBLEDeviceInfoByAddress(gatt.getDevice().getAddress());
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to Sparkle.");
                Log.i(TAG, "Attempting to start service discovery:" +
                        devInfo.mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from Sparkle.");
//                broadcastUpdate(ACTION_DISCONNECTED);
                updateState(devInfo, BLEDeviceInfo.STATE_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BLEDeviceInfo devInfo = GetBLEDeviceInfoByAddress(gatt.getDevice().getAddress());
            Log.d("BLEManager", "Found status " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                devInfo.mBluetoothGattService = gatt.getService(UUID_SERVICE);
                Log.d("BLEManager", "Looking for service " + UUID_SERVICE);

                ArrayList<BluetoothGattService> services = (ArrayList<BluetoothGattService>) gatt.getServices();
                Iterator<BluetoothGattService> it = services.iterator();
                while (it.hasNext()) {
                    BluetoothGattService obj = it.next();
                    Log.d("BLEManager", "Found GATT service " + obj.getUuid());
                }

                if (devInfo.mBluetoothGattService == null) {
                    Log.e(TAG, "Sparkle GATT service not found!");
                    return;
                }

                BluetoothGattCharacteristic receiveCharacteristic =
                        devInfo.mBluetoothGattService.getCharacteristic(UUID_RECEIVE);

                ArrayList<BluetoothGattCharacteristic> characteristics = (ArrayList<BluetoothGattCharacteristic>) devInfo.mBluetoothGattService.getCharacteristics();
                Iterator<BluetoothGattCharacteristic> iter = characteristics.iterator();
                while (iter.hasNext()) {
                    BluetoothGattCharacteristic obj = iter.next();
                    Log.d("BLEManager", "Found GATT characteristic " + obj.getUuid());
                    BluetoothGattDescriptor descr =
                            obj.getDescriptor(obj.getUuid());
                    if (descr != null) {
                        Log.d("BLEManager", "GATT characteristic has descriptor" + descr.toString());
                    } else {
                        Log.d("BLEManager", "GATT characteristic has no descriptor");
                    }
                }


                if (receiveCharacteristic != null) {
                    ArrayList<BluetoothGattDescriptor> descriptors = (ArrayList<BluetoothGattDescriptor>) receiveCharacteristic.getDescriptors();
                    Iterator<BluetoothGattDescriptor> dIter = descriptors.iterator();
                    while (dIter.hasNext()) {
                        BluetoothGattDescriptor obj = dIter.next();
                        Log.d("BLEManager", "Found GATT descriptor " + obj.getUuid());
                    }
                    BluetoothGattDescriptor receiveConfigDescriptor =
                            receiveCharacteristic.getDescriptor(UUID_CLIENT_CONFIGURATION);
                    if (receiveConfigDescriptor != null) {
                        gatt.setCharacteristicNotification(receiveCharacteristic, true);
                        receiveConfigDescriptor.setValue(
                                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(receiveConfigDescriptor);
                        Log.d("BLEManager", "Descriptr Notified");
                    } else {
                        Log.e(TAG, "Sparkle receive config descriptor not found!");
                    }

                } else {
                    Log.e(TAG, "Sparkle receive characteristic not found!");
                }

//                broadcastUpdate(ACTION_CONNECTED);
                updateState(devInfo, BLEDeviceInfo.STATE_CONNECTED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
//        	Log.d("BLEManager", "Characteristic Read");
            if (status == BluetoothGatt.GATT_SUCCESS) {
//            	Log.d("DEBUG", "Data available");
                BLEDeviceInfo devInfo = GetBLEDeviceInfoByAddress(gatt.getDevice().getAddress());
//                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                BLEEvent event = new BLEEvent();
                event.BLEEventType = BLEEvent.EVENT_RX_DATA;
                event.State = devInfo.State;
                event.DeviceInfo = devInfo;
                event.Contents = characteristic.getValue();
                Log.d(TAG, "onCharacteristicRead Data read is of size: " + ((byte[]) (event.Contents)).length);
                SendEvent(event);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
//        	Log.d("BLEManager", "Characteristic Read");
            BLEDeviceInfo devInfo = GetBLEDeviceInfoByAddress(gatt.getDevice().getAddress());
//            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            BLEEvent event = new BLEEvent();
            event.BLEEventType = BLEEvent.EVENT_RX_DATA;
            event.State = devInfo.State;
            event.DeviceInfo = devInfo;
            event.Contents = characteristic.getValue();
            Log.d(TAG, "onCharacteristicChanged Data read is of size: " + ((byte[]) (event.Contents)).length);
            SendEvent(event);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            BLEDeviceInfo devInfo = GetBLEDeviceInfoByAddress(gatt.getDevice().getAddress());
//        	android.util.Log.d("BLEManager ", "  Millisecinds on event " + System.currentTimeMillis());
            BluetoothGattCharacteristic writeChar =
                    devInfo.mBluetoothGattService.getCharacteristic(UUID_SEND);
            if (writeChar == characteristic) {
//        		android.util.Log.d("BLEManager ", "  Millisecinds success " + System.currentTimeMillis());
                devInfo.transmissionDone = true;
            }

        }
    };
    //flags for keeping track of scanning state
    private boolean scanStarted;
    //bluetooth variables
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    public BLEManager(Context context) {
        this.context = context;

        bleDevices = new BLEDeviceInfoList();

        initialize();

        //set up the bluetooth registers and start scanning
        BroadcastReceiver scanModeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean scanning;
                if (bluetoothAdapter == null) {
                    scanning = false;
                } else {
                    scanning = (bluetoothAdapter == null || bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_NONE);
                    scanStarted &= scanning;
                }
                //TO DO
                //Send a BLEEvent with the new state
//            BLEEvent e = new BLEEvent();
//    		e.Type = "State Changed";
//    		SendEvent(e);
            }
        };
        context.registerReceiver(scanModeReceiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                if (state == BluetoothAdapter.STATE_ON) {
//                upgradeState(BLEDeviceInfo.STATE_DISCONNECTED);
                } else if (state == BluetoothAdapter.STATE_OFF) {
//                downgradeState(BLEDeviceInfo.STATE_BLUETOOTH_OFF);
                }
            }
        };
        context.registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
//        context.registerReceiver(sparkleReceiver, getIntentFilter());

        Log.d("DEBUG", "Bluetooth Registers Setup!");
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        updateState(bluetoothAdapter.isEnabled() ? STATE_DISCONNECTED : STATE_BLUETOOTH_OFF);

        scanner = new BLEScanner();
    }

    static public BLEDeviceInfoList GetList() {
        return scanner.newDevices;
    }

    static public boolean send(BLEDeviceInfo devInfo, byte[] data, byte[] header) {
        if (devInfo.mBluetoothGatt == null || devInfo.mBluetoothGattService == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return false;
        }

        BluetoothGattCharacteristic characteristic =
                devInfo.mBluetoothGattService.getCharacteristic(UUID_SEND);

        if (characteristic == null) {
            Log.w(TAG, "Send characteristic not found");
            return false;
        }

        boolean success = false;
        int maxChunk = 960;
        for (int chunkPointer = 0; chunkPointer < data.length; chunkPointer += maxChunk) {

            int chunkLength = (data.length - chunkPointer > maxChunk ? maxChunk : data.length - chunkPointer);

            if (header != null) {
                characteristic.setValue(header);
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                devInfo.transmissionDone = false;

//            android.util.Log.d("BLEManager ", "  Millisecinds before send " + System.currentTimeMillis());
                devInfo.mBluetoothGatt.writeCharacteristic(characteristic);
                while (!devInfo.transmissionDone) {
                }
                Log.d("BLEManager ", "Sent Header");
            }

            byte[] buffer = new byte[20];
            for (int i = 0; i < chunkLength; i += 20) {
                int size = (chunkLength - i > 20 ? 20 : chunkLength - i);
                byte[] tmpBuffer = new byte[size];
                int originalIndex = 0;
                for (int j = i; j < i + size; j++) {
                    tmpBuffer[originalIndex] = data[chunkPointer + j];
                    originalIndex++;
                }
//                Log.d("BLEManager", "Sending down this many bytes on BLE: " + tmpBuffer.length);

                StringBuilder sb = new StringBuilder();
                for (byte b : tmpBuffer) {
                    sb.append(String.format("%02X ", b));
                }
//    	    Log.d("BLEManager", "Sending Down: " + sb.toString());

                characteristic.setValue(tmpBuffer);
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                devInfo.transmissionDone = false;

//                android.util.Log.d("BLEManager ", "  Millisecinds before send " + System.currentTimeMillis());
                success = devInfo.mBluetoothGatt.writeCharacteristic(characteristic);
                while (!devInfo.transmissionDone) {
                }
//                Log.d("BLEManager", "Success of send: " + success);
            }
            byte[] eosBuffer = {0x03, 0x04};
            characteristic.setValue(eosBuffer);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            devInfo.transmissionDone = false;

//            android.util.Log.d("BLEManager ", "  Millisecinds before send " + System.currentTimeMillis());
            success = devInfo.mBluetoothGatt.writeCharacteristic(characteristic);
            while (!devInfo.transmissionDone) {
            }
            Log.d("BLEManager ", "Sent EOS");
            Log.d("BLEManager ", Integer.toString(chunkPointer) + " bytes have gone down so far");
        }
        return success;
    }

    private static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CONNECTED);
        filter.addAction(ACTION_DISCONNECTED);
        filter.addAction(ACTION_DATA_AVAILABLE);
        return filter;
    }

//	private void upgradeState(int newState) {
//        if (newState > state) {
//            updateState(newState);
//        }
//    }
//	private void downgradeState(int newState) {
//        if (newState < state) {
//            updateState(newState);
//        }
//    }

    public BLEDeviceInfo GetBLEDeviceInfoByAddress(String address) {
        return scanner.newDevices.GetBLEDeviceInfoByAddress(address);
    }

    //TO DO: DELETE
    //Old code from before we created the BLEScanner class
//    @Override
//    public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
//    	Log.d("DEBUG", "Got scanrecord " + scanRecord);
//        Log.d("DEBUG", "Got results from scan, device " + device.getName());
//        bluetoothDevice = device;
//        
//        BLEDeviceInfo bleDevice = new BLEDeviceInfo(bluetoothDevice, rssi);
//        bleDevices.InsertOrUpdate(bleDevice);
//        BLEEvent e = new BLEEvent();
//		e.BLEEventType = BLEEvent.EVENT_UPDATE;
//		e.Contents = bleDevices;
//		SendEvent(e);
//    }

    public void start() {
        /*bluetoothAdapter.startLeScan(
                new UUID[]{ this.UUID_SERVICE },
                this);
        Log.d("DEBUG", "BLE Scan Started");*/
        scanner.addObserver(this);
        Thread scannerThread = new Thread(scanner);
        scannerThread.start();
    }

    public void stop() {
        /*bluetoothAdapter.stopLeScan(this);*/
        scanner.deleteObserver(this);
        scanner.stop();
    }

    private void updateState(BLEDeviceInfo devInfo, int newState) {
        devInfo.State = newState;
        //TO DO
        //Send a BLEEvent with the new state
        Log.d(TAG, "Changing state to " + newState);
        BLEEvent e = new BLEEvent();
        e.BLEEventType = BLEEvent.EVENT_DEVICE_STATE_CHANGE;
        e.State = newState;
        e.Contents = scanner.newDevices;
        e.DeviceInfo = devInfo;
        SendEvent(e);
    }

//    private final BroadcastReceiver sparkleReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = intent.getAction();
//            if (ACTION_CONNECTED.equals(action)) {
//                upgradeState(BLEDeviceInfo.STATE_CONNECTED);
//            } else if (ACTION_DISCONNECTED.equals(action)) {
//                downgradeState(BLEDeviceInfo.STATE_DISCONNECTED);
//            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
//            	BLEEvent event = new BLEEvent();
//            	event.BLEEventType = BLEEvent.EVENT_RX_DATA;
//                event.State = state;
//            	event.Contents = intent.getByteArrayExtra(EXTRA_DATA);
//            	SendEvent(event);
//            }
//        }
//    };

    private void SendEvent(BLEEvent e) {
//    	Log.d("DEBUG", "Sending event up");
        setChanged();
        notifyObservers(e);
    }

    @Override
    public void update(Observable observable, Object data) {
        Log.d("DEBUG", "Received event from BLEScanner");
        BLEEvent e = new BLEEvent();
        e.BLEEventType = BLEEvent.EVENT_UPDATE;
        e.Contents = (BLEDeviceInfoList) data;
        SendEvent(e);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        context.sendBroadcast(intent, Manifest.permission.BLUETOOTH);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        if (UUID_RECEIVE.equals(characteristic.getUuid())) {
            final Intent intent = new Intent(action);
//            Log.d("BLEManager", "Got back ");

            byte[] data = characteristic.getValue();

//            Log.d("BLEManager", "Length of data: " + data.length);
            String str;
            try {
                str = new String(data, "UTF-8");
//				Log.d("BLEManager", str);
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
            context.sendBroadcast(intent, Manifest.permission.BLUETOOTH);
        }
    }

    //
//    public class LocalBinder extends Binder {
//        GloveService getService() {
//            return GloveService.this;
//        }
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        return mBinder;
//    }
//
//    @Override
//    public boolean onUnbind(Intent intent) {
//        // After using a given device, you should make sure that BluetoothGatt.close() is called
//        // such that resources are cleaned up properly.  In this particular example, close() is
//        // invoked when the UI is disconnected from the Service.
//        close();
//        return super.onUnbind(intent);
//    }
//
//    private final IBinder mBinder = new LocalBinder();
//
//    /**
//     * Initializes a reference to the local Bluetooth adapter.
//     *
//     * @return Return true if the initialization is successful.
//     */
    private void initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return;
        }

    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void connect(final String address) {
        BLEDeviceInfo devInfo = GetBLEDeviceInfoByAddress(address);

        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return;
        }

        // Previously connected device.  Try to reconnect.
//        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
//                && mBluetoothGatt != null) {
//            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
//            return mBluetoothGatt.connect();
//        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        devInfo.mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect(String address) {
        BLEDeviceInfo devInfo = GetBLEDeviceInfoByAddress(address);
        if (mBluetoothAdapter == null || devInfo.mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        devInfo.mBluetoothGatt.disconnect();
    }

    //    /**
//     * After using a given BLE device, the app must call this method to ensure resources are
//     * released properly.
//     */
//    public void close() {
//        if (mBluetoothGatt == null) {
//            return;
//        }
//        mBluetoothGatt.close();
//        mBluetoothGatt = null;
//    }
//
//    public void read() {
//        if (mBluetoothGatt == null || mBluetoothGattService == null) {
//            Log.w(TAG, "BluetoothGatt not initialized");
//            return;
//        }
//
//        BluetoothGattCharacteristic characteristic =
//                mBluetoothGattService.getCharacteristic(UUID_RECEIVE);
//
//        mBluetoothGatt.readCharacteristic(characteristic);
//    }
//
    public boolean isInitialized(String address) {
        BLEDeviceInfo devInfo = GetBLEDeviceInfoByAddress(address);
        return !(devInfo.mBluetoothGatt == null || devInfo.mBluetoothGattService == null);
    }
}
