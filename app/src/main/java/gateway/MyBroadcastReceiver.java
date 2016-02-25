package gateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Log.d("DEBUG", "Got the reboot command, starting foneRino");
        Intent startServiceIntent = new Intent(context, BLEService.class);
        context.startService(startServiceIntent);
    }
}