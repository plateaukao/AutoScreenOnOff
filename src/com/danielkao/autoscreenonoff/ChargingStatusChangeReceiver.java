package com.danielkao.autoscreenonoff;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static android.content.Intent.ACTION_POWER_CONNECTED;
import static android.content.Intent.ACTION_POWER_DISCONNECTED;

/**
 * Created by plateau on 2013/05/20.
 */
public class ChargingStatusChangeReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {

        // not enabled
        if(!ConstantValues.getPrefChargingOn(context))
            return;

        String action = intent.getAction();
        if(action.equals(ACTION_POWER_CONNECTED)) {
        //if(isCharging){

            ConstantValues.logv("is charging");

            Intent i = new Intent(ConstantValues.SERVICE_INTENT_ACTION);
            i.putExtra(ConstantValues.SERVICEACTION,
                    ConstantValues.SERVICEACTION_TURNON);
            i.putExtra(ConstantValues.SERVICETYPE,
                    ConstantValues.SERVICETYPE_CHARGING);
            context.startService(i);
        }
        else if(action.equals(ACTION_POWER_DISCONNECTED)) {
        //else{

            ConstantValues.logv("is not charging anymore");

            Intent i = new Intent(ConstantValues.SERVICE_INTENT_ACTION);
            i.putExtra(ConstantValues.SERVICEACTION,
                    ConstantValues.SERVICEACTION_TURNOFF);
            i.putExtra(ConstantValues.SERVICETYPE,
                    ConstantValues.SERVICETYPE_CHARGING);
            context.startService(i);

        }
    }


}
