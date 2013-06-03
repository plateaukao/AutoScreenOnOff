package com.danielkao.autoscreenonoff;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by plateau on 2013/06/03.
 */
public class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {

        ConstantValues.logv("boot receiver");

        // auto pref is on
        if(ConstantValues.getPrefAutoOnoff(context)){
            ConstantValues.logv("start service by boot receiver");
            // send intent to service
            Intent i = new Intent(ConstantValues.SERVICE_INTENT_ACTION);
            i.putExtra(ConstantValues.SERVICEACTION,
                    ConstantValues.SERVICEACTION_TOGGLE);
            i.putExtra(ConstantValues.SERVICETYPE,
                    ConstantValues.SERVICETYPE_SETTING);
            context.startService(i);
        }// check whether pre charging is on, and is under charging
        else if(ConstantValues.getPrefChargingOn(context) && ConstantValues.isPlugged(context)){
            Intent i = new Intent(ConstantValues.SERVICE_INTENT_ACTION);
            i.putExtra(ConstantValues.SERVICEACTION,
                    ConstantValues.SERVICEACTION_TURNON);
            i.putExtra(ConstantValues.SERVICETYPE,
                    ConstantValues.SERVICETYPE_CHARGING);
            context.startService(i);
        }
    }
}
