package BLEManagement;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

/**
 * Created by pscot on 2/24/2016.
 */
public class Utilities {

    private static final String TAG = "BluzUtils";

    public Utilities() {
    }

    public boolean checkForBluetooth() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "BLE not supported");
            return false;
        } else if (!mBluetoothAdapter.isEnabled()) {
            Log.e(TAG, "BLE not switched on...");
            return false;
        } else {

            return true;
        }
    }

}
