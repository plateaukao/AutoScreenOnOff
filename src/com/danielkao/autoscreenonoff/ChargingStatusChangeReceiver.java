package com.danielkao.autoscreenonoff;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.widget.Toast;

import static android.content.Intent.ACTION_POWER_CONNECTED;
import static android.content.Intent.ACTION_POWER_DISCONNECTED;

/**
 * Created by plateau on 2013/05/20.
 */
public class ChargingStatusChangeReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        String action = intent.getAction();
        if(action.equals(ACTION_POWER_CONNECTED)) {
        //if(isCharging){

            ConstantValues.logv("is charging");

            Intent i = new Intent(ConstantValues.SERVICE_INTENT_ACTION);
            i.putExtra(ConstantValues.SERVICEACTION,
                    ConstantValues.SERVICEACTION_TURNON);
            context.startService(i);
        }
        else if(action.equals(ACTION_POWER_DISCONNECTED)) {
        //else{
            ConstantValues.logv("is not charging anymore");

            Intent i = new Intent(ConstantValues.SERVICE_INTENT_ACTION);
            i.putExtra(ConstantValues.SERVICEACTION,
                    ConstantValues.SERVICEACTION_TURNOFF);
            context.startService(i);

        }


    }
}
